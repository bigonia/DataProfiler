好的，我们来创建一份《数据库分析实现指南》，其中包含为`RawProfileDataDto`提供数据所需的SQL指令集。

本文档将分别针对MySQL, SQL Server, 和 SQLite 进行说明，并结合我们之前讨论的**自适应剖析策略**，充分考虑执行效率和资源消耗。

-----

### **数据库分析实现指南 (SQL指令集)**

#### **1. 概述**

本文档为平台`IDatabaseProfiler`接口的具体实现者提供标准的SQL指令集参考。其核心目标是高效、准确地获取填充`RawProfileDataDto`所需的数据。

所有实现都应遵循平台的**自适应剖析策略**，该策略分为两个阶段：

  * **阶段一：元数据预检 (Metadata Pre-flight Check)**

      * **目的**: 以极低的成本快速获取库、表、列的定义，以及表的**估算行数**。
      * **作用**: 用于构建UI界面供用户选择，并作为后续执行路径决策的依据。

  * **阶段二：指标获取 (Metrics Acquisition)**

      * **目的**: 根据预检获取的估算行数和内部阈值，选择不同的路径来获取详细的统计指标。
      * **精确执行路径 (Exact Path)**: 针对行数小于阈值的小表，执行全量聚合SQL以获取精确指标。
      * **近似执行路径 (Approximate Path)**: 针对行数大于或等于阈值的大表，通过采样等方式获取近似指标，以保证性能和安全。

-----

### **2. MySQL 实现指南**

#### **阶段一：元数据预检**

1.  **获取指定Schema下的所有表及其估算行数**

      * **说明**: `information_schema.tables`中的`TABLE_ROWS`为InnoDB存储引擎提供了较好的行数估算。
      * **SQL指令**:
        ```sql
        SELECT
            table_name,
            table_comment AS comment,
            table_rows AS rowCount
        FROM
            information_schema.tables
        WHERE
            table_schema = ?; -- 参数为数据库/Schema名称
        ```

2.  **获取指定表的所有列定义**

      * **说明**: 通过`information_schema.columns`获取列的基础信息，并通过`LEFT JOIN`获取主键信息。
      * **SQL指令**:
        ```sql
        SELECT
            c.column_name AS columnName,
            c.column_type AS columnType,
            IF(k.constraint_name = 'PRIMARY', TRUE, FALSE) AS isPrimaryKey,
            c.column_comment AS comment
        FROM
            information_schema.columns c
        LEFT JOIN
            information_schema.key_column_usage k ON c.table_schema = k.table_schema
            AND c.table_name = k.table_name
            AND c.column_name = k.column_name
            AND k.constraint_name = 'PRIMARY'
        WHERE
            c.table_schema = ? AND c.table_name = ?; -- 参数为Schema名和表名
        ```

#### **阶段二：指标获取**

  * **精确执行路径 (`rowCount` \< 阈值)**

      * **说明**: 通过一次查询，动态构建所有列的聚合计算，以减少数据库交互次数。应用层需要根据列名动态生成`SELECT`子句。
      * **SQL指令模板**:
        ```sql
        SELECT
            -- 为每个列生成COUNT(DISTINCT), MIN, MAX聚合
            COUNT(DISTINCT `order_id`) AS `order_id_distinctCount`,
            MIN(`order_id`) AS `order_id_min`,
            MAX(`order_id`) AS `order_id_max`,

            COUNT(DISTINCT `order_amount`) AS `order_amount_distinctCount`,
            MIN(`order_amount`) AS `order_amount_min`,
            MAX(`order_amount`) AS `order_amount_max`
            -- ... 为其他列重复此模式 ...
        FROM
            `your_table_name`;
        ```
      * **注意**: `nullCount`和`rowCount`可以通过`COUNT(*)`和`COUNT(column)`的差值在应用层计算，或在此SQL中增加`COUNT(*) - COUNT(column)`子句。

  * **近似执行路径 (`rowCount` \>= 阈值)**

      * **说明**: 获取数据样本。MySQL没有内建的高效采样语法。`ORDER BY RAND()`性能很差，应避免使用。推荐使用`WHERE RAND() < percentage`的方式，虽然仍需全表扫描，但避免了文件排序，性能更优。
      * **SQL指令 (获取1000行样本)**:
        ```sql
        -- 这里的0.01是一个示例采样率，可以动态调整以获取期望的样本量
        SELECT * FROM `your_table_name` WHERE RAND() < 0.01 LIMIT 1000;
        ```
      * **获取`MIN/MAX`**: 从获取到的样本数据中，在**应用层内存**中计算。

-----

### **3. SQL Server 实现指南**

#### **阶段一：元数据预检**

1.  **获取指定Schema下的所有表及其估算行数**

      * **说明**: 使用`sys`系统视图通常比`INFORMATION_SCHEMA`更高效且信息更丰富。
      * **SQL指令**:
        ```sql
        SELECT
            t.name AS tableName,
            p.rows AS rowCount,
            ep.value AS comment
        FROM
            sys.tables t
        INNER JOIN
            sys.schemas s ON t.schema_id = s.schema_id
        INNER JOIN
            sys.partitions p ON t.object_id = p.object_id AND p.index_id IN (0, 1)
        LEFT JOIN
            sys.extended_properties ep ON t.object_id = ep.major_id AND ep.minor_id = 0 AND ep.class = 1 AND ep.name = 'MS_Description'
        WHERE
            s.name = ?; -- 参数为Schema名称
        ```

2.  **获取指定表的所有列定义**

      * **说明**: 同样使用系统视图组合获取。
      * **SQL指令**:
        ```sql
        SELECT
            c.name AS columnName,
            TYPE_NAME(c.user_type_id) AS columnType,
            CAST(IIF(ic.object_id IS NOT NULL, 1, 0) AS BIT) AS isPrimaryKey,
            ep.value AS comment
        FROM
            sys.columns c
        LEFT JOIN
            sys.index_columns ic ON ic.object_id = c.object_id AND ic.column_id = c.column_id AND ic.is_included_column = 0
        LEFT JOIN
            sys.indexes i ON i.object_id = ic.object_id AND i.index_id = ic.index_id AND i.is_primary_key = 1
        LEFT JOIN
            sys.extended_properties ep ON c.object_id = ep.major_id AND c.column_id = ep.minor_id AND ep.class = 1 AND ep.name = 'MS_Description'
        WHERE
            c.object_id = OBJECT_ID(?); -- 参数为 'schemaName.tableName'
        ```

#### **阶段二：指标获取**

  * **精确执行路径 (`rowCount` \< 阈值)**

      * **说明**: 与MySQL类似，动态构建聚合查询。
      * **SQL指令模板**:
        ```sql
        SELECT
            COUNT_BIG(DISTINCT [order_id]) AS [order_id_distinctCount],
            MIN([order_id]) AS [order_id_min],
            MAX([order_id]) AS [order_id_max],
            -- ... 为其他列重复此模式 ...
            COUNT_BIG(DISTINCT [order_amount]) AS [order_amount_distinctCount],
            MIN([order_amount]) AS [order_amount_min],
            MAX([order_amount]) AS [order_amount_max]
        FROM
            [your_schema_name].[your_table_name];
        ```

  * **近似执行路径 (`rowCount` \>= 阈值)**

      * **说明**: SQL Server提供了高效的`TABLESAMPLE`语法，应优先使用。`SYSTEM`模式基于数据页采样，速度非常快。
      * **SQL指令 (获取约1000行样本)**:
        ```sql
        -- 百分比可以动态计算，例如 (1000 * 100.0 / rowCount)
        SELECT TOP 1000 * FROM [your_schema_name].[your_table_name] TABLESAMPLE SYSTEM (1 PERCENT);
        ```

-----

### **4. SQLite 实现指南**

#### **阶段一：元数据预检**

1.  **获取所有表**

      * **说明**: SQLite没有Schema概念，也没有内置的表注释。`sqlite_master`是标准的查询方式。
      * **SQL指令**:
        ```sql
        SELECT
            name AS tableName
        FROM
            sqlite_master
        WHERE
            type = 'table' AND name NOT LIKE 'sqlite_%';
        ```

2.  **获取表的行数**

      * **说明**: SQLite**没有**提供可靠的行数估算方法，必须执行`COUNT(*)`。这意味着对于SQLite，**自适应策略的预检阶段成本更高**，需要为每个表执行一次`COUNT(*)`来获取行数并用于决策。
      * **SQL指令**:
        ```sql
        SELECT COUNT(*) FROM your_table_name;
        ```

3.  **获取指定表的所有列定义**

      * **说明**: 使用`PRAGMA table_info`是SQLite的标准做法。这不是一条标准的SQL查询，其结果需要由应用层代码进行解析。
      * **指令**:
        ```
        PRAGMA table_info('your_table_name');
        ```
      * **返回结果解析**: 应用层解析返回的`pk`列（\>0表示为主键部分）和`name`, `type`等列。

#### **阶段二：指标获取**

  * **精确执行路径 (`rowCount` \< 阈值)**

      * **说明**: 与MySQL类似，动态构建聚合查询。
      * **SQL指令模板**:
        ```sql
        SELECT
            COUNT(DISTINCT "order_id") AS "order_id_distinctCount",
            MIN("order_id") AS "order_id_min",
            MAX("order_id") AS "order_id_max"
            -- ... 为其他列重复此模式 ...
        FROM
            "your_table_name";
        ```

  * **近似执行路径 (`rowCount` \>= "阈值")**

      * **说明**: 较新版本的SQLite支持`TABLESAMPLE`，但为保证兼容性，`ORDER BY RANDOM()`是更通用的方法。考虑到SQLite通常用于处理本地或中小型数据集（尤其是在本平台中用于处理文件），`ORDER BY RANDOM()`的性能开销通常是可接受的。
      * **SQL指令 (获取1000行样本)**:
        ```sql
        SELECT * FROM "your_table_name" ORDER BY RANDOM() LIMIT 1000;
        ```
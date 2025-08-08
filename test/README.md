# 智能数据剖析与处理平台 - 集成测试

本目录包含了智能数据剖析与处理平台的集成测试脚本和相关配置文件。

> 📖 **详细使用指南**：请参阅 [测试执行指南.md](./测试执行指南.md) 获取完整的测试执行说明和故障排除指南。

## 文件结构

```
test/
├── README.md              # 本文件
├── test_config.json       # 测试配置文件
├── test_runner.py         # 主测试脚本
├── run_tests.bat          # Windows批处理脚本
├── requirements.txt       # Python依赖文件
├── data/
│   └── SalesData.csv      # 测试数据文件
├── sample_data.csv        # 示例测试数据
├── 测试执行指南.md         # 测试执行详细指南
└── 集成测试.md             # 详细测试方案文档
```

## 环境要求

1. **Python 3.x** 环境
2. **requests** 库：`pip install requests`
3. **运行中的后端服务**：确保平台后端服务在 `http://localhost:8080` 运行

## 使用方法

### 1. 完整生命周期测试（默认）

执行所有模块的完整测试流程，包括数据源管理、任务执行、报告查询和文件上传：

```bash
cd test
python test_runner.py --module all
```

### 2. 数据源模块测试

仅测试数据源管理相关功能（创建、连接测试、更新、删除）：

```bash
cd test
python test_runner.py --module datasource
```

### 3. 任务执行模块测试

测试数据剖析任务的完整生命周期（创建、监控、删除）：

```bash
cd test
python test_runner.py --module profiling
```

### 4. 报告查询模块测试

测试报告查询相关功能（摘要报告、详细报告）：

```bash
cd test
python test_runner.py --module report
```

### 5. 文件上传生命周期测试

测试文件上传到报告查询的完整流程：

```bash
cd test
python test_runner.py --module file
```

### 6. 自定义配置文件

使用自定义的配置文件：

```bash
cd test
python test_runner.py --config my_config.json --module all
```

### 7. 使用批处理脚本（Windows）

在Windows环境下，可以使用提供的批处理脚本：

```cmd
REM 运行所有测试
run_tests.bat

REM 运行指定模块测试
run_tests.bat datasource

REM 使用自定义配置文件
run_tests.bat all my_config.json
```

## 配置说明

### test_config.json

```json
{
  "api_base_url": "http://localhost:8080/api/v1",
  "sample_file_path": "./data/SalesData.csv",
  "test_data_sources": [
    {
      "config_name": "TestMySQL",
      "payload": {
        "name": "My Test MySQL DB",
        "type": "MYSQL",
        "properties": {
          "host": "mysql-test-host",
          "port": 3306,
          "username": "test_user",
          "password": "test_password",
          "database": "test_db"
        }
      }
    }
  ]
}
```

- `api_base_url`: 后端API的基础URL
- `sample_file_path`: 用于文件上传测试的样本文件路径
- `test_data_sources`: 数据源测试配置列表

## 测试输出示例

### 数据源模块测试输出
```
[INFO] Running Data Source Module Test
==================================================
[PASS] Step 1: Create Data Source (Status: 201)
[PASS] Step 2: Test Connection (Status: 200)
[PASS] Step 3: Get Data Source (Status: 200)
[PASS] Step 4: Update Data Source (Status: 200)
[PASS] Step 5: Verify Update (Status: 200)
[PASS] Step 5: Name update verified
[PASS] Step 6: Delete Data Source (Status: 204)
[PASS] Step 7: Verify Deletion (Status: 404)

==================================================
SUMMARY:
Tests Run: 8, Passed: 8, Failed: 0
```

### 完整生命周期测试输出
```
[INFO] Running All Test Modules
============================================================

==================== Data Source Module ====================
[INFO] Running Data Source Module Test
==================================================
[PASS] Step 1: Create Data Source (Status: 201)
[PASS] Step 2: Test Connection (Status: 200)
...
[PASS] Data Source Module test passed

==================== Profiling Module ====================
[INFO] Running Profiling Module Test
==================================================
[PASS] Step 1: Create Data Source (Status: 201)
[PASS] Step 2: Start Profiling Task (Status: 201)
[INFO] Task status: RUNNING, waiting...
[PASS] Step 3: Task completed successfully
[PASS] Step 4: Delete Profiling Task (Status: 204)
...
[PASS] Profiling Module test passed

==================================================
SUMMARY:
Tests Run: 32, Passed: 32, Failed: 0
[SUCCESS] All tests passed!
```

## 故障排除

1. **连接错误**：确保后端服务正在运行且可访问
2. **文件不存在**：检查 `sample_file_path` 配置是否正确
3. **权限错误**：确保测试脚本有读取测试数据文件的权限
4. **超时错误**：可能需要调整任务轮询的超时时间

## 扩展测试

要添加新的测试模块，可以：

1. 在 `TestRunner` 类中添加新的测试方法（如 `test_new_module()`）
2. 在 `main()` 函数的 `choices` 列表中添加新模块名称
3. 在 `main()` 函数中添加对应的条件分支处理
4. 在 `run_all_tests()` 方法的 `modules` 列表中添加新模块
5. 在配置文件中添加相应的测试数据

### 测试方法命名规范
- 模块测试：`test_{module_name}_module()`
- 生命周期测试：`test_{feature}_lifecycle()`
- 返回值：`bool` 类型，表示测试是否成功

## 注意事项

- 测试脚本会自动清理创建的测试数据
- 确保测试环境与生产环境隔离
- 定期更新测试数据以反映实际使用场景
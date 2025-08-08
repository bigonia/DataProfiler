package com.dataprofiler.repository;

import com.dataprofiler.entity.DataSourceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for DataSourceConfig entity
 * Provides data access methods for data source configurations
 */
@Repository
public interface DataSourceConfigRepository extends JpaRepository<DataSourceConfig, Long> {

    // --- 自定义条件查询 (Custom Conditional Queries) ---

    /**
     * 根据数据源类型查找所有数据源配置。
     * Spring Data JPA会根据方法名自动生成 "WHERE type = ?" 的查询。
     *
     * @param type 数据源类型, 例如 "FILE", "MYSQL"。
     * @return 对应类型的数据源配置列表。
     */
    List<DataSourceConfig> findByType(String type);

    /**
     * 根据数据源名称进行模糊查询（忽略大小写）。
     * Spring Data JPA会根据方法名自动生成 "WHERE lower(name) LIKE lower(?)" 的查询。
     *
     * @param nameFragment 名称中包含的片段。
     * @return 名称匹配的数据源配置列表。
     */
    List<DataSourceConfig> findByNameContainingIgnoreCase(String nameFragment);


    /**
     * Find data source configuration by source ID
     * @param sourceId the unique source identifier
     * @return Optional containing the data source if found
     */
    DataSourceConfig findBySourceId(String sourceId);

    /**
     * Delete data source configuration by source ID
     * @param sourceId the unique source identifier
     */
    @Modifying
    @Query("DELETE FROM DataSourceConfig d WHERE d.sourceId = :sourceId")
    void deleteBySourceId(@Param("sourceId") String sourceId);

    /**
     * Check if data source exists by source ID
     * @param sourceId the unique source identifier
     * @return true if exists, false otherwise
     */
    boolean existsBySourceId(String sourceId);

    /**
     * 根据数据源类型删除所有匹配的配置。
     * <p>
     * <b>注意:</b>
     * <ul>
     * <li>{@code @Modifying}: 必须添加此注解，以告知Spring Data JPA这是一个会修改数据库状态（DELETE/UPDATE/INSERT）的查询。</li>
     * <li>{@code @Query}: 由于方法名删除（如 deleteByType）有时会与框架内置逻辑冲突或不够清晰，
     * 使用@Query注解可以明确地定义操作。这里使用JPQL (Java Persistence Query Language)。</li>
     * <li><b>事务性:</b> 调用此方法的Service层方法应添加 {@code @Transactional} 注解以确保事务的正确性。</li>
     * </ul>
     *
     * @param type 需要删除的数据源类型。
     * @return 被删除的记录数量。
     */
//    @Modifying
//    @Query("DELETE FROM DataSourceConfig d WHERE d.type = :type")
    int deleteByType(@Param("type") String type);

}
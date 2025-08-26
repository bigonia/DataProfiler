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

    void deleteById(Long id);

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

}
package com.dataprofiler.profiler.impl;

import com.dataprofiler.dto.internal.RawProfileDataDto;
import com.dataprofiler.dto.request.ProfilingTaskRequest;
import com.dataprofiler.entity.DataSourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MySqlProfiler
 * Tests profiling functionality with real MySQL database connection
 */
@ExtendWith(MockitoExtension.class)
class MySqlProfilerTest {

    private MySqlProfiler profiler;
    private DataSourceConfig dataSourceConfig;
    private ProfilingTaskRequest.DataSourceScope scope;

    @BeforeEach
    void setUp() {
        profiler = new MySqlProfiler();
        
        // Create test data source configuration
        dataSourceConfig = new DataSourceConfig();
        dataSourceConfig.setSourceId("test-mysql-ds");
        dataSourceConfig.setName("Test MySQL DataSource");
        dataSourceConfig.setType(DataSourceConfig.DataSourceType.MYSQL);
        
        // Set connection properties
        Map<String, String> properties = new HashMap<>();
        properties.put("host", "127.0.0.1");
        properties.put("port", "3306");
        properties.put("username", "root");
        properties.put("password", "root");
        properties.put("database", "pb-cms-base");
        dataSourceConfig.setProperties(properties);
        
        // Create test scope - profile all tables
        scope = new ProfilingTaskRequest.DataSourceScope();
        scope.setSchemas(new HashMap<>()); // Empty means all tables
    }

    @Test
    void testGetSupportedType() {
        assertEquals("MYSQL", profiler.getSupportedType());
    }

    @Test
    void testSupports() {
        assertTrue(profiler.supports("MYSQL"));
        assertTrue(profiler.supports("mysql"));
        assertTrue(profiler.supports("MySQL"));
        assertFalse(profiler.supports("POSTGRESQL"));
        assertFalse(profiler.supports("SQLSERVER"));
    }

    @Test
    void testTestConnection() {
        // Test connection with valid configuration
        boolean connectionResult = profiler.testConnection(dataSourceConfig);
        assertTrue(connectionResult, "Connection should be successful with valid configuration");
        
        // Test connection with invalid configuration
        DataSourceConfig invalidConfig = new DataSourceConfig();
        invalidConfig.setSourceId("invalid-ds");
        invalidConfig.setType(DataSourceConfig.DataSourceType.MYSQL);
        
        Map<String, String> invalidProperties = new HashMap<>();
        invalidProperties.put("host", "invalid-host");
        invalidProperties.put("port", "3306");
        invalidProperties.put("username", "invalid-user");
        invalidProperties.put("password", "invalid-password");
        invalidProperties.put("database", "invalid-database");
        invalidConfig.setProperties(invalidProperties);
        
        boolean invalidConnectionResult = profiler.testConnection(invalidConfig);
        assertFalse(invalidConnectionResult, "Connection should fail with invalid configuration");
    }

    @Test
    void testProfileWithEmptyScope() throws Exception {
        // Test profiling with empty scope (should profile all tables)
        RawProfileDataDto result = profiler.profile(dataSourceConfig, scope);
        
        assertNotNull(result, "Profile result should not be null");
        assertEquals("test-mysql-ds", result.getDataSourceId());
        assertEquals(DataSourceConfig.DataSourceType.MYSQL, result.getDataSourceType());
        assertEquals("pb-cms-base", result.getDatabaseName());
        assertNotNull(result.getProfilingTimestamp());
        
        // Check metadata
        assertNotNull(result.getMetadata());
        assertTrue(result.getMetadata().containsKey("mysql_version"));
        assertTrue(result.getMetadata().containsKey("total_tables_profiled"));
        
        // Check tables
        assertNotNull(result.getTables());
        assertTrue(result.getTables().size() > 0, "Should have profiled at least one table");
        
        // Verify first table structure
        RawProfileDataDto.TableData firstTable = result.getTables().get(0);
        assertNotNull(firstTable.getTableName());
        assertNotNull(firstTable.getSchemaName());
        assertNotNull(firstTable.getRowCount());
        assertTrue(firstTable.getRowCount() >= 0);
        
        // Check columns
        assertNotNull(firstTable.getColumns());
        if (!firstTable.getColumns().isEmpty()) {
            RawProfileDataDto.ColumnData firstColumn = firstTable.getColumns().get(0);
            assertNotNull(firstColumn.getColumnName());
            assertNotNull(firstColumn.getDataType());
            assertNotNull(firstColumn.getNullable());
        }
        
        // Check indexes
        assertNotNull(firstTable.getIndexes());
    }

    @Test
    void testProfileWithSpecificScope() throws Exception {
        // Create scope for specific schema and tables
        Map<String, List<String>> schemas = new HashMap<>();
        // Assuming there's a table named 'user' or similar in the test database
        // You may need to adjust this based on actual tables in pb-cms-base
        schemas.put("pb-cms-base", Arrays.asList()); // Empty list means all tables in schema
        
        ProfilingTaskRequest.DataSourceScope specificScope = new ProfilingTaskRequest.DataSourceScope();
        specificScope.setSchemas(schemas);
        
        RawProfileDataDto result = profiler.profile(dataSourceConfig, specificScope);
        
        assertNotNull(result);
        assertEquals("test-mysql-ds", result.getDataSourceId());
        assertEquals(DataSourceConfig.DataSourceType.MYSQL, result.getDataSourceType());
        assertNotNull(result.getTables());
    }

    @Test
    void testProfileWithNullScope() throws Exception {
        // Test profiling with null scope (should profile all tables)
        RawProfileDataDto result = profiler.profile(dataSourceConfig, null);
        
        assertNotNull(result);
        assertEquals("test-mysql-ds", result.getDataSourceId());
        assertEquals(DataSourceConfig.DataSourceType.MYSQL, result.getDataSourceType());
        assertNotNull(result.getTables());
    }

    @Test
    void testProfileColumnDataTypes() throws Exception {
        RawProfileDataDto result = profiler.profile(dataSourceConfig, scope);
        
        assertNotNull(result);
        assertNotNull(result.getTables());
        
        // Find a table with columns to test
        Optional<RawProfileDataDto.TableData> tableWithColumns = result.getTables().stream()
            .filter(table -> table.getColumns() != null && !table.getColumns().isEmpty())
            .findFirst();
        
        if (tableWithColumns.isPresent()) {
            RawProfileDataDto.TableData table = tableWithColumns.get();
            
            for (RawProfileDataDto.ColumnData column : table.getColumns()) {
                // Verify basic column properties
                assertNotNull(column.getColumnName());
                assertNotNull(column.getDataType());
                assertNotNull(column.getNullable());
                
                // Verify profiling metrics
                if (column.getTotalCount() != null) {
                    assertTrue(column.getTotalCount() >= 0);
                }
                
                if (column.getNullCount() != null) {
                    assertTrue(column.getNullCount() >= 0);
                }
                
                if (column.getUniqueCount() != null) {
                    assertTrue(column.getUniqueCount() >= 0);
                }
                
                // Verify sample values
                if (column.getSampleValues() != null) {
                    assertTrue(column.getSampleValues().size() <= 10);
                }
            }
        }
    }

    @Test
    void testProfileIndexes() throws Exception {
        RawProfileDataDto result = profiler.profile(dataSourceConfig, scope);
        
        assertNotNull(result);
        assertNotNull(result.getTables());
        
        // Find a table with indexes to test
        Optional<RawProfileDataDto.TableData> tableWithIndexes = result.getTables().stream()
            .filter(table -> table.getIndexes() != null && !table.getIndexes().isEmpty())
            .findFirst();
        
        if (tableWithIndexes.isPresent()) {
            RawProfileDataDto.TableData table = tableWithIndexes.get();
            
            for (RawProfileDataDto.IndexData index : table.getIndexes()) {
                assertNotNull(index.getIndexName());
                assertNotNull(index.getIndexType());
                assertNotNull(index.getIsUnique());
                assertNotNull(index.getIsPrimary());
                assertNotNull(index.getColumnNames());
                assertTrue(index.getColumnNames().size() > 0);
            }
        }
    }

    @Test
    void testProfileWithInvalidDataSource() {
        // Test with invalid data source configuration
        DataSourceConfig invalidConfig = new DataSourceConfig();
        invalidConfig.setSourceId("invalid-ds");
        invalidConfig.setType(DataSourceConfig.DataSourceType.MYSQL);
        
        Map<String, String> invalidProperties = new HashMap<>();
        invalidProperties.put("host", "invalid-host");
        invalidProperties.put("port", "3306");
        invalidProperties.put("username", "invalid-user");
        invalidProperties.put("password", "invalid-password");
        invalidProperties.put("database", "invalid-database");
        invalidConfig.setProperties(invalidProperties);
        
        assertThrows(Exception.class, () -> {
            profiler.profile(invalidConfig, scope);
        }, "Should throw exception with invalid data source configuration");
    }

    @Test
    void testProfileMetadata() throws Exception {
        RawProfileDataDto result = profiler.profile(dataSourceConfig, scope);
        
        assertNotNull(result);
        assertNotNull(result.getMetadata());
        
        // Check MySQL version metadata
        assertTrue(result.getMetadata().containsKey("mysql_version"));
        assertNotNull(result.getMetadata().get("mysql_version"));
        
        // Check total tables profiled metadata
        assertTrue(result.getMetadata().containsKey("total_tables_profiled"));
        Integer totalTables = (Integer) result.getMetadata().get("total_tables_profiled");
        assertNotNull(totalTables);
        assertTrue(totalTables >= 0);
        assertEquals(result.getTables().size(), totalTables.intValue());
    }
}
package com.nexora.platform.service;

import com.nexora.platform.entity.AiApiKey;
import com.nexora.platform.mapper.AiApiKeyMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ApiKeyServiceTest.TestConfig.class)
@Transactional
class ApiKeyServiceTest {

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private AiApiKeyMapper apiKeyMapper;

    @Configuration
    @EnableTransactionManagement
    @MapperScan("com.nexora.platform.mapper")
    @Import(ApiKeyService.class)
    static class TestConfig {

        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("testdb")
                .addScript("classpath:schema.sql")
                .build();
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
            SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
            factoryBean.setDataSource(dataSource);
            org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
            configuration.setMapUnderscoreToCamelCase(true);
            factoryBean.setConfiguration(configuration);
            return factoryBean.getObject();
        }

        @Bean
        SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
            return new SqlSessionTemplate(sqlSessionFactory);
        }
    }

    @Test
    void createApiKey_shouldReturnRawKeyAndStoreHash() {
        ApiKeyService.CreatedKey result = apiKeyService.createApiKey(1L, "PLATFORM", null, "test-key");
        assertNotNull(result.rawKey());
        assertTrue(result.rawKey().startsWith("sk-nex-"), "Raw key should start with sk-nex-");
        assertEquals("test-key", result.name());
        // Verify stored in DB: retrieve by hash and check status
        AiApiKey stored = apiKeyMapper.findByHash(ApiKeyService.hashKey(result.rawKey()));
        assertNotNull(stored, "Stored key should be retrievable by hash");
        assertEquals("ACTIVE", stored.getStatus());
        assertEquals("test-key", stored.getKeyName());
    }

    @Test
    void listApiKeys_shouldReturnUserKeys() {
        apiKeyService.createApiKey(1L, "PLATFORM", null, "key-a");
        apiKeyService.createApiKey(1L, "PLATFORM", null, "key-b");
        List<AiApiKey> keys = apiKeyService.listApiKeys(1L, 1);
        assertFalse(keys.isEmpty(), "Should list created keys for user 1");
        assertTrue(keys.size() <= 20, "Page size should not exceed 20");
    }

    @Test
    void disableApiKey_shouldSetStatusToDisabled() {
        ApiKeyService.CreatedKey key = apiKeyService.createApiKey(1L, "PLATFORM", null, "test");
        boolean result = apiKeyService.disableApiKey(key.id(), 1L);
        assertTrue(result, "Disabling owned key should succeed");
        AiApiKey stored = apiKeyMapper.findById(key.id());
        assertEquals("DISABLED", stored.getStatus());
    }

    @Test
    void disableApiKey_shouldFailForWrongUser() {
        ApiKeyService.CreatedKey key = apiKeyService.createApiKey(1L, "PLATFORM", null, "test");
        boolean result = apiKeyService.disableApiKey(key.id(), 999L); // different user
        assertFalse(result, "Disabling key by wrong user should fail");
        AiApiKey stored = apiKeyMapper.findById(key.id());
        assertEquals("ACTIVE", stored.getStatus(), "Status should remain unchanged");
    }

    @Test
    void hashKey_shouldBeDeterministic() {
        String raw = "sk-nex-test-key-12345";
        String hash1 = ApiKeyService.hashKey(raw);
        String hash2 = ApiKeyService.hashKey(raw);
        assertEquals(hash1, hash2, "Same input should produce same hash");
        assertEquals(64, hash1.length(), "SHA-256 hex output should be 64 characters");
    }
}

package org.uteq.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Configuracion del patron CACHE-ASIDE con Redis.
 *
 * Como funciona con @Cacheable: ante una peticion, Spring consulta primero
 * Redis. Si la clave existe (cache hit) devuelve el valor y NO toca PostgreSQL.
 * Si no existe (cache miss) ejecuta el metodo, guarda el resultado en Redis y
 * lo devuelve. @CacheEvict invalida las entradas cuando los datos cambian, para
 * que la cache no sirva informacion obsoleta.
 *
 * NOTA SOBRE LA SERIALIZACION: se usa JdkSerializationRedisSerializer y no un
 * serializador JSON porque el valor cacheado es un Page<T> (PageImpl). PageImpl
 * no tiene constructor sin argumentos y Jackson falla al DESERIALIZARLO, con un
 * error que solo aparece en el segundo acceso (el primer hit escribe bien y el
 * fallo surge al leer). Requisito: los DTO cacheados deben implementar
 * java.io.Serializable.
 *
 * El bean solo se crea si spring.cache.type != none. Asi el benchmark puede
 * ejecutar la aplicacion SIN cache (SPRING_CACHE_TYPE=none) y CON cache
 * (SPRING_CACHE_TYPE=redis) sin recompilar ni tocar el codigo.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class CacheConfig {

    public static final String CACHE_ESTUDIANTES = "estudiantes";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))     // TTL defensivo: la cache
                                                      // no puede quedar obsoleta
                                                      // para siempre si falla un evict
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new JdkSerializationRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base)
                .withCacheConfiguration(CACHE_ESTUDIANTES,
                        base.entryTtl(Duration.ofMinutes(5)))
                .build();
    }
}

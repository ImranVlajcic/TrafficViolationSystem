package com.academy.trafficviolationsystem.audit;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Enables Spring AOP's @AspectJ support.
 *
 * Without this, @Aspect on AuditAspect is ignored at runtime — the class
 * loads as a regular bean but its @Around advice never fires.
 *
 * proxyTargetClass = true forces CGLIB subclass proxies instead of JDK
 * interface proxies. This is required when intercepting concrete service
 * classes that do not implement an interface (e.g. classes that only
 * implement BaseCRUDService via a default interface method chain).
 *
 * Placed in the audit package so it is loaded only when the audit module
 * is present. Move to core/config/ if you want it to apply globally
 * without explicitly requiring the audit module.
 *
 * Note: Spring Boot auto-configuration enables AOP by default via
 * spring-boot-starter-aop. This class makes it explicit and ensures
 * CGLIB proxies are used. Add the dependency to pom.xml if not present:
 *
 *   &lt;dependency&gt;
 *     &lt;groupId&gt;org.springframework.boot&lt;/groupId&gt;
 *     &lt;artifactId&gt;spring-boot-starter-aop&lt;/artifactId&gt;
 *   &lt;/dependency&gt;
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AopConfig {
    // No beans needed — the annotation does all the work.
}

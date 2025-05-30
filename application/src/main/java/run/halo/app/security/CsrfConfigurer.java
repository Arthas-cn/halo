package run.halo.app.security;

import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

import org.springframework.core.annotation.Order;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfWebFilter;
import org.springframework.security.web.server.csrf.XorServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.security.authentication.SecurityConfigurer;
import run.halo.app.security.authentication.pat.PatAuthenticationConverter;

@Component
@Order(0)
class CsrfConfigurer implements SecurityConfigurer {

    @Override
    public void configure(ServerHttpSecurity http) {
        var csrfMatcher = new AndServerWebExchangeMatcher(
            CsrfWebFilter.DEFAULT_CSRF_MATCHER,
            new NegatedServerWebExchangeMatcher(pathMatchers(
                "/api/**",
                "/apis/**",
                "/actuator/**",
                "/system/setup"
            )),
            new NegatedServerWebExchangeMatcher(patAuthMatcher())
        );
        http.csrf(csrfSpec -> csrfSpec
            .csrfTokenRepository(new CookieServerCsrfTokenRepository())
            .csrfTokenRequestHandler(new XorServerCsrfTokenRequestAttributeHandler())
            .requireCsrfProtectionMatcher(csrfMatcher));
    }

    private static ServerWebExchangeMatcher patAuthMatcher() {
        var patConverter = new PatAuthenticationConverter();
        return exchange -> patConverter.convert(exchange)
            .flatMap(a -> ServerWebExchangeMatcher.MatchResult.match())
            .switchIfEmpty(Mono.defer(ServerWebExchangeMatcher.MatchResult::notMatch));
    }

}

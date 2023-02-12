/*
 * Copyright 2019-2023 The Polypheny Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.polypheny.security.tokenmanagement;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;
import javalinjwt.JWTGenerator;
import javalinjwt.JWTProvider;
import javalinjwt.JavalinJWT;
import org.polypheny.security.authentication.model.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

public class JwtManager {
    private final Algorithm algorithm = Algorithm.HMAC256("very_secret");
    private final JWTGenerator<User> generator = (user, alg) -> {
        JWTCreator.Builder token = JWT.create().withClaim("username", user.getUsername()).withClaim("roles", user.getRoles().toString()).withSubject(user.getUsername()).withIssuedAt(Date.from(Instant.now())).withExpiresAt(Date.from(Instant.now().plus(12, ChronoUnit.HOURS)));
        return token.sign(alg);
    };
    private final JWTVerifier verifier = JWT.require(algorithm).build();
    private final JWTProvider<User> provider = new JWTProvider<>(algorithm, generator, verifier);
    private final Handler decodeHandler = JavalinJWT.createHeaderDecodeHandler(provider);

    public JWTProvider<User> getProvider() {
        return provider;
    }

    public String generateToken(User user) {
        return provider.generateToken(user);
    }

    public Optional<DecodedJWT> extractTokenInformations(Context ctx) {
        String header = ctx.req.getHeader("Authorization");
        if (header != null) {
            String token = header.substring(7);
            Optional<DecodedJWT> decodedJWT = provider.validateToken(token);
            if (decodedJWT.isEmpty()) {
                throw new UnauthorizedResponse();
            } else {
                return decodedJWT;
            }
        } else {
            throw new UnauthorizedResponse();
        }
    }

    public void validateUserRole(Context ctx, DecodedJWT token, String role) {
        String roles = token.getClaim("roles").asString();
        if (!roles.contains(role)) {
            throw new ForbiddenResponse();
        }
    }
}

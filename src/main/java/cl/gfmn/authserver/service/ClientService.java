package cl.gfmn.authserver.service;

import cl.gfmn.authserver.exception.BadRequestException;
import cl.gfmn.authserver.model.Response;
import cl.gfmn.authserver.model.user.CreateClientRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final RegisteredClientRepository registeredClientRepo;
    private final PasswordEncoder passwordEncoder;

    /**
     * Method that creates a user in database
     * @param request CreateClientRequest POJO
     * @return Response POJO
     */
    public Response createClient(CreateClientRequest request) {

        //validate if client exists
        if(registeredClientRepo.findByClientId(request.client_id()) != null)
            throw new BadRequestException("Client already exists");

        registeredClientRepo.save(RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId(request.client_id())
                .clientSecret(passwordEncoder.encode(request.client_secret()))
                .clientAuthenticationMethods(clientAuthenticationMethods -> clientAuthenticationMethods.addAll(transformStringToClassAuthenticationMethods(request.authentication_methods())))
                .authorizationGrantTypes(authorizationGrantTypes -> authorizationGrantTypes.addAll(transformStringToAuthorizationGrantTypes(request.authorization_grant_types())))
                .scopes(scopes -> scopes.addAll(request.scopes()))
                .redirectUris(redirectUris -> redirectUris.addAll(request.scopes()))
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofSeconds(request.access_token_time_to_live()))
                        .build())
                .build());

        return new Response(LocalDateTime.now().toString(), "Client created");
    }

    public Set<ClientAuthenticationMethod> transformStringToClassAuthenticationMethods(List<String> authenticationMethodList) {
        Set<ClientAuthenticationMethod> authenticationMethodSet = new HashSet<>();
        authenticationMethodList.forEach(authenticationMethod -> {
            switch(authenticationMethod) {
                case "client_secret_basic" -> authenticationMethodSet.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                case "client_secret_post" -> authenticationMethodSet.add(ClientAuthenticationMethod.CLIENT_SECRET_POST);
                case "client_secret_jwt" -> authenticationMethodSet.add(ClientAuthenticationMethod.CLIENT_SECRET_JWT);
                case "private_key_jwt" -> authenticationMethodSet.add(ClientAuthenticationMethod.PRIVATE_KEY_JWT);
                default -> throw new BadRequestException("Invalid authentication method = " + authenticationMethod);
            }
        });
        return authenticationMethodSet;
    }

    public Set<AuthorizationGrantType> transformStringToAuthorizationGrantTypes(List<String> authorizationGrantTypesList) {
        Set<AuthorizationGrantType> authorizationGrantTypesSet = new HashSet<>();
        authorizationGrantTypesList.forEach(authorizationGrantType -> {
            switch(authorizationGrantType) {
                case "authorization_code" -> authorizationGrantTypesSet.add(AuthorizationGrantType.AUTHORIZATION_CODE);
                case "refresh_token" -> authorizationGrantTypesSet.add(AuthorizationGrantType.REFRESH_TOKEN);
                case "client_credentials" -> authorizationGrantTypesSet.add(AuthorizationGrantType.CLIENT_CREDENTIALS);
                case "jwt_bearer" -> authorizationGrantTypesSet.add(AuthorizationGrantType.JWT_BEARER);
                case "device_code" -> authorizationGrantTypesSet.add(AuthorizationGrantType.DEVICE_CODE);
                default -> throw new BadRequestException("Invalid authorization grant type = " + authorizationGrantType);
            }
        });
        return authorizationGrantTypesSet;
    }
}

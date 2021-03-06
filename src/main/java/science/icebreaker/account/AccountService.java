package science.icebreaker.account;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import science.icebreaker.config.SecurityConfig;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * Please refer to {@link SecurityConfig} for an overview of the security concept.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;

    private final AccountRoleRepository accountRoleRepository;

    private final AccountProfileRepository accountProfileRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final String jwtSecret;

    private final long jwtTokenValidityMs;


    public AccountService(
            AccountRepository accountRepository,
            AccountRoleRepository accountRoleRepository,
            AccountProfileRepository accountProfileRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            @Value("${icebreaker.jwt-secret}") String jwtSecret,
            @Value("${icebreaker.jwt-token-validity-ms}") long jwtTokenValidityMs) {
        this.accountRepository = accountRepository;
        this.accountRoleRepository = accountRoleRepository;
        this.accountProfileRepository = accountProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtSecret = jwtSecret;
        this.jwtTokenValidityMs = jwtTokenValidityMs;
    }


    /**
     *
     * @param accountId The ID of an account
     * @return The profile of an account
     * @throws AccountNotFoundException If there is no account with the given ID
     */
    public AccountProfile getAccountProfile(int accountId) throws AccountNotFoundException {
        Optional<AccountProfile> profile = accountProfileRepository.findById(accountId);
        if (profile.isEmpty()) {
            throw new AccountNotFoundException(accountId);
        }
        return profile.get();
    }


    /**
     * This function checks if an Account contains the required information to be created. The following fields are
     * expected to be not empty: forename, surname, email, password, institution, city.
     * The email has to have the correct format.
     *
     * @return true if the account is valid; false otherwise
     */
    public boolean validateRegistration(RegistrationRequest registration) {
        Account account = registration.getAccount();
        AccountProfile profile = registration.getProfile();
        return !profile.getForename().isBlank() &&
                !profile.getSurname().isBlank() &&
                !profile.getInstitution().isBlank() &&
                !account.getEmail().isBlank() &&
                !account.getPassword().isBlank() &&
                EmailValidator.getInstance().isValid(account.getEmail());
    }


    /**
     *
     * @param registration The registration data; the ID fields must be null.
     * @return The ID of the new account
     * @throws AccountCreationException If data is missing or if the ID fields are not null.
     */
    public int createAccount(RegistrationRequest registration) throws AccountCreationException {
        Account account = registration.getAccount();
        AccountProfile profile = registration.getProfile();
        if (account.getId() != null || profile.getAccountId() != null) {
            throw new AccountCreationException("The account id is not null.");
        }
        if (!validateRegistration(registration)) {
            throw new AccountCreationException("Validation error");
        }

        account.setEmail(account.getEmail().strip().toLowerCase());
        account.setPassword(passwordEncoder.encode(account.getPassword()));

        // TODO Save the data in a single transaction
        try {
            accountRepository.save(account);
        } catch (DataIntegrityViolationException e) {
            throw new AccountCreationException("The email is already used.");
        }
        int accountId = account.getId();
        accountRoleRepository.save(new AccountRole(account.getEmail(), "USER"));
        profile.setAccountId(accountId);
        accountProfileRepository.save(profile);
        return account.getId();
    }


    /**
     * An exception will be thrown if the login data are wrong. Please look up the documentation of
     * {@link AuthenticationManager#authenticate} to see the possible RuntimeExceptions.
     *
     * @param account The login data; the ID field must be null.
     * @return A new JWT token
     */
    public String login(Account account) {
        if (account.getId() != null) {
            throw new IllegalArgumentException("The account id is not null.");
        }
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(account.getEmail(),
                account.getPassword()));
        return generateJwtToken(account);
    }


    private String generateJwtToken(Account account) {
        Map<String, Object> claims = new HashMap<>();
        int accountId = accountRepository.findAccountByEmail(account.getEmail()).getId();
        claims.put("account_id", accountId);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(account.getEmail())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtTokenValidityMs))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }
}

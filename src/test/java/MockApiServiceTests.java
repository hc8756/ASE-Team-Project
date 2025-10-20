import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import model.Transaction;
import service.MockApiService;
import model.User;
import org.mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * This class contains the unit tests for the MockApiTests class.
 */

public class MockApiServiceTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private MockApiService service;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testViewAllUsers() {
        List<User> users = List.of(new User(), new User());
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(users);
        List<User> test = service.viewAllUsers();
        assertEquals(2, test.size());
    }

    @Test
    public void testViewAllUsersNoUsers() {
        List<User> users = List.of();
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<User>>any())).thenReturn(users);
        List<User> test = service.viewAllUsers();
        assertEquals(0, test.size());
    }

    @Test
    public void testGetUserFound() {
        User user = new User();
        UUID userId = user.getUserId();
        when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(), eq(userId))).thenReturn(user);
    }

    @Test
    public void testGetUserNotFound() {
        User user = new User();
        UUID userId = user.getUserId();
        when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<User>>any(), eq(userId))).thenThrow(new RuntimeException());

        Optional<User> test = service.getUser(userId);
        assertTrue(test.isEmpty());

    }

    @Test
    public void testAddUserSuccess() {
        User user = new User();
        UUID userId = user.getUserId();
        when(jdbcTemplate.queryForObject(anyString(),eq(UUID.class), anyString(), anyString(), anyDouble()))
            .thenReturn(userId);
        
        User test = service.addUser(user);
        assertEquals(userId, test.getUserId());
    }

    @Test
    public void testDeleteUserSuccess() {
        
    }

    @Test
    public void testViewAllTransactions() {
        
    }

    @Test
    public void testGetAllTransactions() {
        
    }

    @Test
    public void testAddTransactionSuccess() {
        
    }

    @Test
    public void testAddTransactionFail() {
        
    }


}

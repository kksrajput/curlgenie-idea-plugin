package curlgenie.service;

//import org.junit.jupiter.api.*;
import com.curlgenie.CurlGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CurlGeneratorTest {

    private CurlGenerator gen;

    @BeforeEach
    void setup() {
        gen = new CurlGenerator();
    }

    @Test
    void testGetHello() {
        String method = "@GetMapping(\"/hello\") public String sayHello(@RequestParam String name) {}";
        String curl = gen.generateCurl(method, "");
        assertTrue(curl.contains("curl -X GET"));
        assertTrue(curl.contains("/hello?name="));
    }

    @Test
    void testPostNestedJson() {
        String method = "@PostMapping(\"/users\") public void create(@RequestBody User u) {}";
        String classes = "public class User { private String name; private int age; private Address address; }"
                + "public class Address { private String city; }";
        String curl = gen.generateCurl(method, classes);
        assertTrue(curl.contains("\"name\""));
        assertTrue(curl.contains("\"address\""));
    }

    @Test
    void testModelAttribute() {
        String method = "@PutMapping(\"/update\") public void update(@ModelAttribute Req r) {}";
        String classes = "public class Req { private String id; private String val; }";
        String curl = gen.generateCurl(method, classes);
        assertTrue(curl.contains("?id="));
        assertTrue(curl.contains("val="));
    }

    @Test
    void testRequestHeader() {
        String method = "@GetMapping(\"/auth\") public void a(@RequestHeader(\"Authorization\") String tok) {}";
        String curl = gen.generateCurl(method, "");
        assertTrue(curl.contains("-H \"Authorization: val\""));
    }

    @Test
    void testMapBody() {
        String method = "@PostMapping(\"/dynamic\") public void d(@RequestBody Map<String,Object> m) {}";
        String curl = gen.generateCurl(method, "");
        assertTrue(curl.contains("{\"key\""));
    }

    @Test
    void testXml() {
        String method = "@PostMapping(value=\"/xml\", consumes=\"application/xml\") public void x(@RequestBody X u) {}";
        String classes = "public class X { private String name; }";
        String curl = gen.generateCurl(method, classes);
        assertTrue(curl.contains("application/xml"));
        assertTrue(curl.contains("<X><name>"));
    }

    @Test
    void testInheritance() {
        String method = "@PostMapping(\"/create\") public void create(@RequestBody ExtendedUser user) {}";
        String classes = "public class BaseUser { private String email; } public class ExtendedUser extends BaseUser { private String username; }";
        String curl = gen.generateCurl(method, classes);
        assertTrue(curl.contains("email"));
        assertTrue(curl.contains("username"));
    }

    @Test
    void testInterfaceField() {
        String method = "@PostMapping(\"/event\") public void handle(@RequestBody Event event) {}";
        String classes = "public interface Event { String getName(); } public class MyEvent implements Event { private String name; }";
        String curl = gen.generateCurl(method, classes);
        assertTrue(curl.contains("\"name\""));
    }

    @Test
    void testRequestMappingEnum() {
        String method = "@RequestMapping(value = \"/complex\", method = RequestMethod.POST) public void handle(@RequestBody Complex c) {}";
        String classes = "public class Complex { private String data; }";
        String curl = gen.generateCurl(method, classes);
        assertTrue(curl.contains("curl -X POST"));
        assertTrue(curl.contains("\"data\""));
    }
}

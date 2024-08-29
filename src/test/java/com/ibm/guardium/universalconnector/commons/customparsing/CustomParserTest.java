package com.ibm.guardium.universalconnector.commons.customparsing;

import org.junit.Assert;
import org.junit.Test;

public class CustomParserTest {
    CustomParser parser = new CustomParser() {
        @Override
        public String getConfigFilePath() {
            return "src/test/java/resources/config.json";
        }
    };

    @Test
    public void test() {
        String payload = "2024-08-23T15:22:35.876Z [INFO] [Audit] [Session ID: 75849321] [Client Port: 53422] [Server Port: 5432] [DB User: admin_user] [Server Type: PostgreSQL] [DB Protocol: TCP/IP] [Exception Type ID: 104] [DB Name: EmployeeDB] [Client IP: 10.0.0.45]\n" +
                "Action: Query Execution\n" +
                "Query: SELECT * FROM employee_records WHERE employee_id = 1123;\n" +
                "Details: Query executed successfully by user 'admin_user'.\n";
        String record = parser.parse(payload, "(?<=\\[Session ID: )\\d+(?=\\])");

        Assert.assertNotNull(record);
    }

    @Test
    public void test2() {
        String payload = "teststring ";
        String record = parser.parse(payload, "(\\w+)\\s");

        Assert.assertNotNull(record);
    }

}
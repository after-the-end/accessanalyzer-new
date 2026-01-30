package org.iam.utils;

import org.iam.common.basetypes.Policy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;

public class PolicyParserTest {
    @Test
    public void testJsonExistence() throws IOException {
        String jsonContent = new String(PolicyParser.getTestFile("org.iam.utils/test.json").readAllBytes());
        Assertions.assertFalse(jsonContent.isEmpty());
    }

    @Test
    public void testPolicyParser() {
        Policy<?> policy = PolicyParser.parseInput(PolicyParser.getTestFile("org.iam.utils/test.json"));
        System.out.println(policy);
        Assertions.assertNotNull(policy);
    }

}

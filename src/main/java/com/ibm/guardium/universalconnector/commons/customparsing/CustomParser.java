package com.ibm.guardium.universalconnector.commons.customparsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.guardium.universalconnector.commons.customparsing.regex.RegexExecutor;
import com.ibm.guardium.universalconnector.commons.customparsing.regex.RegexResult;
import com.ibm.guardium.universalconnector.commons.structures.Accessor;
import com.ibm.guardium.universalconnector.commons.structures.Record;
import com.ibm.guardium.universalconnector.commons.structures.SessionLocator;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ibm.guardium.universalconnector.commons.customparsing.PropertyConstant.*;

public abstract class CustomParser {
    protected static final RegexExecutor executor = new RegexExecutor();
    private static final Logger logger = LogManager.getLogger(CustomParser.class);
    protected static InetAddressValidator inetAddressValidator = InetAddressValidator.getInstance();
    protected Map<String, String> properties;
    ObjectMapper mapper;

    CustomParser() {
    }

    public Record parseRecord(String payload) {
        properties = getProperties();
        if (properties == null || payload == null) return null;

        return extractRecord(payload);
    }

    private Record extractRecord(String payload) {
        Record record = new Record();

        setSessionId(record, payload);

        if (properties.get(DB_NAME) != null)
            record.setDbName(parse(payload, properties.get(DB_NAME)));

        if (properties.get(CALLER_IP) != null)
            record.setSessionLocator(parseSessionLocator(parse(payload, properties.get(CALLER_IP))));

        record.setAccessor(parseAccessor("type", "hostname", "protocol", "serviceName", "appUserName"));

        return record;
    }

    protected void setSessionId(Record record, String payload) {
        String sessionId = parse(payload, properties.get(SESSION_ID));
        if (sessionId != null)
            record.setSessionId(sessionId);
        else
            record.setSessionId(DEFAULT_STRING);
    }

    String parse(String payload, String regexString) {
        Pattern pattern = Pattern.compile(regexString);
        RegexResult rr = executor.find(pattern, payload);
        if (rr.matched()) {
            Matcher m = rr.getMatcher();
            if (m.groupCount() > 0) return m.group(1);
            return m.group();

        } else {
            if (rr.timedOut() && logger.isDebugEnabled()) {
                logger.debug(
                        String.format(
                                "Regex parse aborted due to taking too long to match"
                                        + " -- regex: %s, event-payload: %s",
                                pattern, payload));
            }
            // Else, failed to parse without hitting timeout
            return null;
        }
    }

    Accessor parseAccessor(String serviceType, String hostname, String protocol, String serviceName, String appUserName) {
        Accessor accessor = new Accessor();

        accessor.setServerType(serviceType);
        accessor.setServerOs(UNKOWN_STRING);

        accessor.setClientOs(UNKOWN_STRING);
        accessor.setClientHostName(UNKOWN_STRING);

        accessor.setServerHostName(hostname);

        accessor.setCommProtocol(UNKOWN_STRING);

        accessor.setDbProtocol(protocol);
        accessor.setDbProtocolVersion(UNKOWN_STRING);

        accessor.setOsUser(UNKOWN_STRING);
        accessor.setSourceProgram(UNKOWN_STRING);

        accessor.setClient_mac(UNKOWN_STRING);
        accessor.setServerDescription(UNKOWN_STRING);

        accessor.setLanguage(Accessor.LANGUAGE_FREE_TEXT_STRING);
        accessor.setDataType(Accessor.DATA_TYPE_GUARDIUM_SHOULD_NOT_PARSE_SQL);

        accessor.setDbUser(appUserName);
        accessor.setServiceName(serviceName);
        return accessor;
    }

    SessionLocator parseSessionLocator(String callerIp) {
        SessionLocator sessionLocator = new SessionLocator();
        sessionLocator.setIpv6(false);
        if (inetAddressValidator.isValidInet6Address(callerIp)) {
            sessionLocator.setIpv6(true);
            sessionLocator.setClientIpv6(callerIp);
            sessionLocator.setServerIpv6(DEFAULT_IPV6);
        } else if (inetAddressValidator.isValidInet4Address(callerIp)) {
            sessionLocator.setClientIp(callerIp);
            sessionLocator.setServerIp(DEFAULT_IP);
        } else {
            sessionLocator.setClientIp(DEFAULT_IP);
            sessionLocator.setServerIp(DEFAULT_IP);
        }
        sessionLocator.setClientPort(SessionLocator.PORT_DEFAULT);
        sessionLocator.setServerPort(SessionLocator.PORT_DEFAULT);
        return sessionLocator;
    }

    public abstract Map<String, String> getProperties();

    protected HashMap<String, String> readJsonFileAsJson(String fileName) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(fileName)));
        return (HashMap<String, String>) (new ObjectMapper()).readValue(content, HashMap.class);
    }

}

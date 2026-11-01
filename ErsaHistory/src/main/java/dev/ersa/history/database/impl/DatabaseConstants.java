package dev.ersa.history.database.impl;

public interface DatabaseConstants {
    String SERVERS_TABLE = "ersa_history_servers";
    String CHECK_NAMES_TABLE = "ersa_history_check_names";
    String CLIENT_BRANDS_TABLE = "ersa_history_client_brands";
    String CLIENT_VERSIONS_TABLE = "ersa_history_client_versions";
    String VIOLATIONS_TABLE = "ersa_history_violations";

    String SERVERS_STRING_COLUMN = "server_name";
    String CHECK_NAMES_STRING_COLUMN = "check_name_string";
    String CLIENT_BRANDS_STRING_COLUMN = "client_brand_string";
    String CLIENT_VERSIONS_STRING_COLUMN = "client_version_string";

    String VIOLATIONS_ID_COLUMN = "id";
    String VIOLATIONS_UUID_COLUMN = "uuid";
    String VIOLATIONS_VERBOSE_COLUMN = "verbose";
    String VIOLATIONS_VL_COLUMN = "vl";
    String VIOLATIONS_CREATED_AT_COLUMN = "created_at";
    String VIOLATIONS_SENS_X_COLUMN = "sensitivity_x";
    String VIOLATIONS_SENS_Y_COLUMN = "sensitivity_y";
    String VIOLATIONS_PING_COLUMN = "ping";

    String VIOLATIONS_SERVER_ID_COLUMN = "server_id";
    String VIOLATIONS_CHECK_NAME_ID_COLUMN = "check_name_id";
    String VIOLATIONS_CLIENT_BRAND_ID_COLUMN = "client_brand_id";
    String VIOLATIONS_CLIENT_VERSION_ID_COLUMN = "client_version_id";
}
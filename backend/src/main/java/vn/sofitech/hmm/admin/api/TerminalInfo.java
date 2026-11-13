package vn.sofitech.hmm.admin.api;

import lombok.Value;

@Value
public class TerminalInfo {

    Long terminalId;
    String terminalCode;
    Long storeId;
    String storeCode;
    String storeName;
}

package vn.sofitech.hmm.admin.api;

import lombok.Value;

@Value
public class StoreInfo {

    Long storeId;
    String storeCode;
    String name;
    String address;
}

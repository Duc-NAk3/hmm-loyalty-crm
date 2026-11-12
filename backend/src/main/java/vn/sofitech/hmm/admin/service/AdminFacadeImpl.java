package vn.sofitech.hmm.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.admin.AdminFacade;
import vn.sofitech.hmm.admin.domain.*;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AdminFacadeImpl implements AdminFacade {

    private final PosTerminalRepository terminals;
    private final StoreRepository stores;
    private final AppUserRepository users;
    private final SystemParameterRepository params;

    public AdminFacadeImpl(PosTerminalRepository terminals, StoreRepository stores, AppUserRepository users,
                           SystemParameterRepository params) {
        this.terminals = terminals;
        this.stores = stores;
        this.users = users;
        this.params = params;
    }

    static TerminalInfo toInfo(PosTerminal t) {
        Store s = t.getStore();
        return new TerminalInfo(t.getId(), t.getTerminalCode(), s.getId(), s.getStoreCode(), s.getName());
    }

    @Override
    public Optional<TerminalInfo> findActiveTerminal(long terminalId) {
        return terminals.findByIdAndStatus(terminalId, "ACTIVE").map(AdminFacadeImpl::toInfo);
    }

    @Override
    public Optional<TerminalInfo> findTerminal(long terminalId) {
        return terminals.findById(terminalId).map(AdminFacadeImpl::toInfo);
    }

    @Override
    public Optional<StoreInfo> findStore(long storeId) {
        return stores.findById(storeId).map(s -> new StoreInfo(s.getId(), s.getStoreCode(), s.getName(), s.getAddress()));
    }

    @Override
    public Optional<UserInfo> findUser(long userId) {
        return users.findById(userId).map(u -> new UserInfo(u.getId(), u.getUsername(), u.getFullName()));
    }

    @Override
    public int intParam(String key, int defaultValue) {
        return params.findById(key).map(p -> Integer.parseInt(p.getValue())).orElse(defaultValue);
    }
}

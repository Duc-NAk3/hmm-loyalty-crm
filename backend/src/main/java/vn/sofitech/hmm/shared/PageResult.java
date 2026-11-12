package vn.sofitech.hmm.shared;

import java.util.List;

/** Kết quả phân trang trả cho client. */
public record PageResult<T>(long total, List<T> items) {
}

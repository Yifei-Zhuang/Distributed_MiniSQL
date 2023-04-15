package master.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

enum StatusCode {
    SUCCESS,
    FAIL
}


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Response {
    public StatusCode statusCode;
    public String msg;

    public static MyResponse success() {
        return new MyResponse(StatusCode.SUCCESS, "");
    }

    public static MyResponse success(String msg) {
        return new MyResponse(StatusCode.SUCCESS, msg);
    }

    public static MyResponse fail() {
        return new MyResponse(StatusCode.FAIL, "");
    }

    public static MyResponse fail(String msg) {
        return new MyResponse(StatusCode.FAIL, msg);
    }
}
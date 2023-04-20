package master.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Response {
    public StatusCode statusCode;
    public String msg;

    public static Response success() {
        return new Response(StatusCode.SUCCESS, "");
    }

    public static Response success(String msg) {
        return new Response(StatusCode.SUCCESS, msg);
    }

    public static Response fail() {
        return new Response(StatusCode.FAIL, "");
    }

    public static Response fail(String msg) {
        return new Response(StatusCode.FAIL, msg);
    }
}
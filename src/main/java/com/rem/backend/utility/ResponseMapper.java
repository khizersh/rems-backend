package com.rem.backend.utility;

import java.util.HashMap;
import java.util.Map;

import static com.rem.backend.utility.Utility.*;

public class ResponseMapper {

    public static Map<String, Object> buildResponse(Responses responses, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put(RESPONSE_CODE, responses.getResponseCode());
        response.put(RESPONSE_MESSAGE, responses.getResponseMessage());
        response.put(DATA, data);
        return response;
    }

}

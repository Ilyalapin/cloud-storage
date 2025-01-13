package com.cloud_storage.common;

import com.cloud_storage.common.exception.UserAlreadeExistException;
import com.cloud_storage.common.exception.UserNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import static jakarta.servlet.http.HttpServletResponse.*;
@WebFilter("/*")
public class ExceptionHandlingFilter extends HttpFilter {
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        try {
            super.doFilter(req, res, chain);
        } catch (UserAlreadeExistException e) {
            writeErrorResponse(res, SC_CONFLICT, e);
        } catch (UserNotFoundException e) {
            writeErrorResponse(res, SC_NOT_FOUND, e);
        }
//        catch (NotFoundException e) {
//            writeErrorResponse(res, SC_NOT_FOUND, e);
//        } catch (SessionNotInitializedException e) {
//            writeErrorResponse(res, SC_FORBIDDEN, e);
//        } catch (HttpSessionInvalidatedException e) {
//            writeErrorResponse(res, SC_UNAUTHORIZED, e);
//        }
    }


    private void writeErrorResponse(HttpServletResponse response, int errorCode, RuntimeException e) throws
            IOException {
        response.setStatus(errorCode);
        objectMapper.writeValue(response.getWriter(), new ErrorResponseDto(errorCode, e.getMessage()));
    }
}

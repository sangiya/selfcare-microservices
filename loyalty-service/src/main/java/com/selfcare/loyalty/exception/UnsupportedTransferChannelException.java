package com.selfcare.loyalty.exception;

import com.selfcare.platform.common.web.ApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a transfer channel (e.g. FlySmiles, Amex) is not enabled for this operator. */
public class UnsupportedTransferChannelException extends ApiException {
    public UnsupportedTransferChannelException(String message) {
        super("UNSUPPORTED_TRANSFER_CHANNEL", message, HttpStatus.BAD_REQUEST);
    }
}

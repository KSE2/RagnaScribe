package org.ragna.exception;

import java.io.IOException;

public class UnknownPathException extends IOException {

   public UnknownPathException() {
   }

   public UnknownPathException(String message) {
      super(message);
   }

   public UnknownPathException(Throwable cause) {
      super(cause);
   }

   public UnknownPathException(String message, Throwable cause) {
      super(message, cause);
   }

}

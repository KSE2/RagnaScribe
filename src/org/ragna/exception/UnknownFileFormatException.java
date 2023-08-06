package org.ragna.exception;

import java.io.IOException;

public class UnknownFileFormatException extends IOException {

   public UnknownFileFormatException() {
   }

   public UnknownFileFormatException(String message) {
      super(message);
   }

   public UnknownFileFormatException(Throwable cause) {
      super(cause);
   }

   public UnknownFileFormatException(String message, Throwable cause) {
      super(message, cause);
   }

}

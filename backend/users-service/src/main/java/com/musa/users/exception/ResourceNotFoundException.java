package com.musa.users.exception;

/**Excepción lanzada cuando no se encuentra un recurso o entidad solicitada en la base de datos.*/
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

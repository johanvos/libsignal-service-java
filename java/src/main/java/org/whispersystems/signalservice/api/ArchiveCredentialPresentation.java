package org.whispersystems.signalservice.api;

/**
 *
 * @author johan
 */
public record ArchiveCredentialPresentation (byte[] presentation, byte[] signedPresentation) {
    
}
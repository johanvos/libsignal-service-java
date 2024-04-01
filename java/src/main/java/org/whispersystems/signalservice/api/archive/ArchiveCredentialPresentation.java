package org.whispersystems.signalservice.api.archive;

/**
 *
 * @author johan
 */
public record ArchiveCredentialPresentation (byte[] presentation, byte[] signedPresentation) {
    
}
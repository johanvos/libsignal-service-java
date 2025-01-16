package org.whispersystems.signalservice.api.crypto;


import java.util.Optional;
import java.util.logging.Logger;

public class UnidentifiedAccessPair {

  private final Optional<UnidentifiedAccess> targetUnidentifiedAccess;
  private final Optional<UnidentifiedAccess> selfUnidentifiedAccess;
    private static final Logger LOG = Logger.getLogger(UnidentifiedAccessPair.class.getName());

    public UnidentifiedAccessPair(UnidentifiedAccess targetUnidentifiedAccess, UnidentifiedAccess selfUnidentifiedAccess) {
        this.targetUnidentifiedAccess = Optional.ofNullable(targetUnidentifiedAccess);
        this.selfUnidentifiedAccess = Optional.ofNullable(selfUnidentifiedAccess);
    }

  public Optional<UnidentifiedAccess> getTargetUnidentifiedAccess() {
    return targetUnidentifiedAccess;
  }

  public Optional<UnidentifiedAccess> getSelfUnidentifiedAccess() {
    return selfUnidentifiedAccess;
  }
}

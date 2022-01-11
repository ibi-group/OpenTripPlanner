package org.opentripplanner.transit.raptor.api.transit;


/**
 * Raptor does not need any information from the constrained transfer, but it passes the
 * instance in a callback to the cost calculator.
 */
public interface RaptorTransferConstraint {

    /**
     * A regular transfer is a transfer with no constraints.
     */
    RaptorTransferConstraint REGULAR_TRANSFER = new RaptorTransferConstraint() {
        @Override public boolean isNotAllowed() { return false; }
        @Override public boolean isRegularTransfer() { return true; }

        @Override
        public int getMinTransferTime() {
            return 0;
        }
    };


    /**
     * Return {@code true} if the constrained transfer is not allowed between the two routes.
     * Note! If a constraint only apply to specific trips, then the
     * {@link RaptorConstrainedTripScheduleBoardingSearch} is reponsible for NOT returning the
     * NOT-ALLOWED transfer, and finding the next ALLOWED trip.
     */
    boolean isNotAllowed();

    /**
     * Returns {@code true} if this is a regular transfer without any constrains.
     */
    boolean isRegularTransfer();

    /**
     * TODO - Clean up according to implementation
     * The min-transfer-time specifies a lower bound for the transfer time. Raptor uses this to make
     * sure at least the amount of seconds specified is available to transfer. If the is
     * higher than the actual path transfer time (e.g. walking time), the .
     */
    int getMinTransferTime();
}
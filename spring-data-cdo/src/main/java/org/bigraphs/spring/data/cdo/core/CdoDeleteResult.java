package org.bigraphs.spring.data.cdo.core;

import org.springframework.lang.NonNull;

/**
 * @author Dominik Grzelak
 */
public abstract class CdoDeleteResult {
    public abstract boolean wasAcknowledged();

    public abstract long getDeletedCount();

    public static CdoDeleteResult acknowledged(final long deletedCount) {
        return new AcknowledgedCdoDeleteResult(deletedCount);
    }

    public static CdoDeleteResult unacknowledged() {
        return new UnacknowledgedCdoDeleteResult();
    }

    public static CdoDeleteResult unacknowledged(@NonNull Throwable reason) {
        return new UnacknowledgedCdoDeleteResult(reason);
    }

    public static class AcknowledgedCdoDeleteResult extends CdoDeleteResult {
        private final long deletedCount;

        public AcknowledgedCdoDeleteResult(long deletedCount) {
            this.deletedCount = deletedCount;
        }

        @Override
        public boolean wasAcknowledged() {
            return true;
        }

        @Override
        public long getDeletedCount() {
            return deletedCount;
        }

        @Override
        public String toString() {
            return "AcknowledgedCdoDeleteResult{" +
                    "deletedCount=" + deletedCount +
                    '}';
        }
    }

    public static class UnacknowledgedCdoDeleteResult extends CdoDeleteResult {

        private Throwable reason;

        public UnacknowledgedCdoDeleteResult() {
        }

        public UnacknowledgedCdoDeleteResult(Throwable reason) {
            this.reason = reason;
        }

        public Throwable getReason() {
            return reason;
        }

        @Override
        public boolean wasAcknowledged() {
            return false;
        }

        @Override
        public long getDeletedCount() {
            return 0;
        }

        @Override
        public String toString() {
            return "UnacknowledgedCdoDeleteResult{" +
                    "reason=" + reason.getMessage() +
                    '}';
        }
    }
}

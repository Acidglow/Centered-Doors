package acidglow.centereddoors.block;

import net.minecraft.util.StringRepresentable;

public enum DoorDepth implements StringRepresentable {
    FRONT("front"),
    MIDDLE_TO_BACK("middle_back"),
    BACK("back"),
    MIDDLE_TO_FRONT("middle_front");

    private final String serializedName;

    DoorDepth(String serializedName) {
        this.serializedName = serializedName;
    }

    public DoorDepth next() {
        return switch (this) {
            case FRONT -> MIDDLE_TO_BACK;
            case MIDDLE_TO_BACK -> BACK;
            case BACK -> MIDDLE_TO_FRONT;
            case MIDDLE_TO_FRONT -> FRONT;
        };
    }

    public double minZ() {
        return switch (this) {
            case FRONT -> 13.0;
            case MIDDLE_TO_BACK, MIDDLE_TO_FRONT -> 6.5;
            case BACK -> 0.0;
        };
    }

    public double maxZ() {
        return switch (this) {
            case FRONT -> 16.0;
            case MIDDLE_TO_BACK, MIDDLE_TO_FRONT -> 9.5;
            case BACK -> 3.0;
        };
    }

    public String modelSuffix() {
        return switch (this) {
            case FRONT -> "front";
            case MIDDLE_TO_BACK, MIDDLE_TO_FRONT -> "middle";
            case BACK -> "back";
        };
    }

    public boolean isMiddle() {
        return this == MIDDLE_TO_BACK || this == MIDDLE_TO_FRONT;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}

package com.genos.overlay.model;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Parameters for action commands
 */
public class CommandParameters implements Parcelable {
    private PointF position; // For tap, long-press
    private PointF startPosition; // For swipe start
    private PointF endPosition; // For swipe end
    private String text; // For type_text
    private String direction; // For scroll, swipe
    private Integer nodeIndex; // For targeting specific UI elements
    private String nodeText; // For targeting UI elements by text
    private Integer nodeId; // For targeting UI elements by ID
    private Long durationMs; // For swipe duration
    private Integer scrollAmount; // For scroll commands

    public CommandParameters() {
    }

    protected CommandParameters(Parcel in) {
        if (in.readByte() == 0) {
            position = null;
        } else {
            position = new PointF(in.readFloat(), in.readFloat());
        }
        if (in.readByte() == 0) {
            startPosition = null;
        } else {
            startPosition = new PointF(in.readFloat(), in.readFloat());
        }
        if (in.readByte() == 0) {
            endPosition = null;
        } else {
            endPosition = new PointF(in.readFloat(), in.readFloat());
        }
        text = in.readString();
        direction = in.readString();
        if (in.readByte() == 0) {
            nodeIndex = null;
        } else {
            nodeIndex = in.readInt();
        }
        nodeText = in.readString();
        if (in.readByte() == 0) {
            nodeId = null;
        } else {
            nodeId = in.readInt();
        }
        if (in.readByte() == 0) {
            durationMs = null;
        } else {
            durationMs = in.readLong();
        }
        if (in.readByte() == 0) {
            scrollAmount = null;
        } else {
            scrollAmount = in.readInt();
        }
    }

    public static final Creator<CommandParameters> CREATOR = new Creator<CommandParameters>() {
        @Override
        public CommandParameters createFromParcel(Parcel in) {
            return new CommandParameters(in);
        }

        @Override
        public CommandParameters[] newArray(int size) {
            return new CommandParameters[size];
        }
    };

    // Builder pattern for easier parameter construction
    public static class Builder {
        private CommandParameters params = new CommandParameters();

        public Builder position(float x, float y) {
            params.position = new PointF(x, y);
            return this;
        }

        public Builder startPosition(float x, float y) {
            params.startPosition = new PointF(x, y);
            return this;
        }

        public Builder endPosition(float x, float y) {
            params.endPosition = new PointF(x, y);
            return this;
        }

        public Builder text(String text) {
            params.text = text;
            return this;
        }

        public Builder direction(String direction) {
            params.direction = direction;
            return this;
        }

        public Builder nodeIndex(int nodeIndex) {
            params.nodeIndex = nodeIndex;
            return this;
        }

        public Builder nodeText(String nodeText) {
            params.nodeText = nodeText;
            return this;
        }

        public Builder nodeId(int nodeId) {
            params.nodeId = nodeId;
            return this;
        }

        public Builder duration(Long durationMs) {
            params.durationMs = durationMs;
            return this;
        }

        public Builder scrollAmount(Integer scrollAmount) {
            params.scrollAmount = scrollAmount;
            return this;
        }

        public CommandParameters build() {
            return params;
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    // Getters and Setters
    public PointF getPosition() {
        return position;
    }

    public void setPosition(PointF position) {
        this.position = position;
    }

    public PointF getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(PointF startPosition) {
        this.startPosition = startPosition;
    }

    public PointF getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(PointF endPosition) {
        this.endPosition = endPosition;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Integer getNodeIndex() {
        return nodeIndex;
    }

    public void setNodeIndex(Integer nodeIndex) {
        this.nodeIndex = nodeIndex;
    }

    public String getNodeText() {
        return nodeText;
    }

    public void setNodeText(String nodeText) {
        this.nodeText = nodeText;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Integer getScrollAmount() {
        return scrollAmount;
    }

    public void setScrollAmount(Integer scrollAmount) {
        this.scrollAmount = scrollAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandParameters that = (CommandParameters) o;
        return Objects.equals(position, that.position) &&
                Objects.equals(startPosition, that.startPosition) &&
                Objects.equals(endPosition, that.endPosition) &&
                Objects.equals(text, that.text) &&
                Objects.equals(direction, that.direction) &&
                Objects.equals(nodeIndex, that.nodeIndex) &&
                Objects.equals(nodeText, that.nodeText) &&
                Objects.equals(nodeId, that.nodeId) &&
                Objects.equals(durationMs, that.durationMs) &&
                Objects.equals(scrollAmount, that.scrollAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, startPosition, endPosition, text, direction, 
                           nodeIndex, nodeText, nodeId, durationMs, scrollAmount);
    }

    @Override
    public String toString() {
        return "CommandParameters{" +
                "position=" + position +
                ", startPosition=" + startPosition +
                ", endPosition=" + endPosition +
                ", text='" + text + '\'' +
                ", direction='" + direction + '\'' +
                ", nodeIndex=" + nodeIndex +
                ", nodeText='" + nodeText + '\'' +
                ", nodeId=" + nodeId +
                ", durationMs=" + durationMs +
                ", scrollAmount=" + scrollAmount +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (position == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeFloat(position.x);
            dest.writeFloat(position.y);
        }
        if (startPosition == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeFloat(startPosition.x);
            dest.writeFloat(startPosition.y);
        }
        if (endPosition == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeFloat(endPosition.x);
            dest.writeFloat(endPosition.y);
        }
        dest.writeString(text);
        dest.writeString(direction);
        if (nodeIndex == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(nodeIndex);
        }
        dest.writeString(nodeText);
        if (nodeId == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(nodeId);
        }
        if (durationMs == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(durationMs);
        }
        if (scrollAmount == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(scrollAmount);
        }
    }
}
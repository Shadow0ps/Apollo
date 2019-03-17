package com.apollocurrency.aplwallet.apl.core.db.dao.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * Shard db entity
 */
public class Shard {
    private Long shardId;
    private byte[] shardHash;

    public Shard() {
    }

    public Shard copy() {
        byte[] shardHashCopy = Arrays.copyOf(shardHash, shardHash.length);
        return new Shard(shardId, shardHashCopy);
    }

    public Shard(byte[] shardHash) {
        this.shardHash = shardHash;
    }

    public Shard(Long shardId, byte[] shardHash) {
        this.shardId = shardId;
        this.shardHash = shardHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Shard shard = (Shard) o;
        return Objects.equals(shardId, shard.shardId) &&
                Arrays.equals(shardHash, shard.shardHash);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(shardId);
        result = 31 * result + Arrays.hashCode(shardHash);
        return result;
    }

    public Long getShardId() {
        return shardId;
    }

    public void setShardId(Long shardId) {
        this.shardId = shardId;
    }

    public byte[] getShardHash() {
        return shardHash;
    }

    public void setShardHash(byte[] shardHash) {
        this.shardHash = shardHash;
    }

    public static ShardBuilder builder() {
        return new ShardBuilder();
    }

    public static final class ShardBuilder {
        private Long shardId;
        private byte[] shardHash;

        private ShardBuilder() {
        }

        public ShardBuilder id(Long shardId) {
            this.shardId = shardId;
            return this;
        }

        public ShardBuilder shardHash(byte[] shardHash) {
            this.shardHash = shardHash;
            return this;
        }

        public Shard build() {
            return new Shard(shardId, shardHash);
        }
    }
}

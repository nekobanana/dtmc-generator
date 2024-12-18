package com.github.nekobanana.dtmcgenerator.sampling.sampler;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nekobanana.dtmcgenerator.sampling.aliasmethod.AliasTable;
import com.github.nekobanana.dtmcgenerator.utils.RandomUtils;
import org.la4j.Matrix;
import java.util.Random;

public abstract class Sampler {
    protected final int n;
    protected final Matrix P;
    protected AliasTable[] aliasTables;
    @JsonIgnore
    protected Random rand;
    @JsonIgnore
    protected ObjectMapper mapper;

    public Sampler(Matrix P) {
        assert (P.rows() == P.columns());
        this.n = P.rows();
        this.P = P;
        this.rand = RandomUtils.rand;
        this.aliasTables = new AliasTable[n];
        setupAliasTables();
        mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    private void setupAliasTables() {
        for (int state = 0; state < n; state++) {
            aliasTables[state] = new AliasTable(P.getRow(state).toDenseVector().toArray());
        }
    }

    protected int generateNextStateNumber(int state, int randomInt, double randomDouble) {
        return aliasTables[state].sample(randomInt, randomDouble);
    }

    public int getN() {return n;}

    abstract public void reset();
}

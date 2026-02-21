package cn.zbx1425.nquestmod.data.criteria;

import cn.zbx1425.nquestmod.data.quest.StepState;

public record CriterionContext(StepState state, String path) {

    public CriterionContext child(String segment) {
        return new CriterionContext(state,
            path.isEmpty() ? segment : path + "/" + segment);
    }

    public CriterionContext child(int index) {
        return child(String.valueOf(index));
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return state.getBoolean(path, key, defaultValue);
    }

    public void setBoolean(String key, boolean value) {
        state.setBoolean(path, key, value);
    }

    public double getDouble(String key, double defaultValue) {
        return state.getDouble(path, key, defaultValue);
    }

    public void setDouble(String key, double value) {
        state.setDouble(path, key, value);
    }

    public int getInt(String key, int defaultValue) {
        return state.getInt(path, key, defaultValue);
    }

    public void setInt(String key, int value) {
        state.setInt(path, key, value);
    }
}

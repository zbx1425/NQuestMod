package cn.zbx1425.nquestmod.data;

import cn.zbx1425.nquestmod.data.criteria.CriteriaRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class NQuestGson {

    public static final Gson INSTANCE = new GsonBuilder()
            .registerTypeAdapterFactory(CriteriaRegistry.getFactory())
            .create();

    public static final Gson PRETTY = new GsonBuilder()
            .registerTypeAdapterFactory(CriteriaRegistry.getFactory())
            .setPrettyPrinting()
            .create();
}

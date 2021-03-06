package com.alipay.simplehbase.hql.node;

import java.util.Map;

import com.alipay.simplehbase.hql.HQLNode;
import com.alipay.simplehbase.hql.HQLNodeType;
import com.alipay.simplehbase.util.Util;
import com.sun.istack.internal.Nullable;

public class StatementNode extends HQLNode {
    protected StatementNode() {
        super(HQLNodeType.Statement);
    }

    @Override
    public void applyParaMap(@Nullable Map<String, Object> para,
            StringBuilder sb, Map<Object, Object> context) {
        Util.checkNull(sb);
        Util.checkNull(context);

        for (HQLNode hqlNode : subNodeList) {
            hqlNode.applyParaMap(para, sb, context);
        }

    }

}

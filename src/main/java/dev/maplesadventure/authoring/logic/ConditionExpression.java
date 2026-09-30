package dev.maplesadventure.authoring.logic;
import java.util.*;
public record ConditionExpression(Kind kind, LogicDefinition leaf, List<ConditionExpression> children) {
    public enum Kind { ALL, ANY, NOT, LEAF }
    public ConditionExpression {
        Objects.requireNonNull(kind); children=List.copyOf(children);
        if(children.size()>LogicLimits.NODES||(kind==Kind.LEAF?(leaf==null||!children.isEmpty()):(leaf!=null))
                ||(kind==Kind.NOT&&children.size()!=1))throw new IllegalArgumentException("Malformed condition expression");
        check(kind,children,1,new int[]{1});
    }
    private static void check(Kind kind,List<ConditionExpression> children,int depth,int[] count){
        if(depth>LogicLimits.DEPTH||count[0]>LogicLimits.NODES)throw new IllegalArgumentException("Condition tree limit");
        for(var c:children){count[0]++;check(c.kind,c.children,depth+1,count);}
    }
    public static ConditionExpression all(){return new ConditionExpression(Kind.ALL,null,List.of());}
    public static ConditionExpression leaf(LogicDefinition d){return new ConditionExpression(Kind.LEAF,d,List.of());}
}

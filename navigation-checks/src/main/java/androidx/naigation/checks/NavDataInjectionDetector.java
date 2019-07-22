package androidx.naigation.checks;

import androidx.naigation.checks.dao.FunDao;
import androidx.naigation.checks.dao.NavConstraintsDao;
import androidx.naigation.checks.util.Constants;
import androidx.naigation.checks.util.UsefulMethods;

import com.android.SdkConstants;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import com.android.utils.Pair;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class NavDataInjectionDetector extends ResourceXmlDetector {

    private Map<String,String> syn_map = null;
    private List<Pair<String,String>> influences;
    private List<Pair<String,String>> influences_tran_closure;
    private List<String> exported;
    private Map<String, List<String>> scope = null;
    private Map<String, String> graph_startDest_map = null;
    private Map<String, String> graph_layout_map = null;
    private Map<String, String> activity_layout_map = null;
    private List<String> malicious = null;
    List<FunDao> expOfIntrst = null;

    public static final Issue DATA_INJECTION_SCREEN = Issue.create("DataInjectionScreen",
            "Detects malicious arguments",
            "Detects arguments accepted by exported activities that can be used to influence" +
                    "critical resources accessed or owned by the app",
            Category.LINT, 10, Severity.ERROR,
            new Implementation(NavDataInjectionDetector.class, Scope.RESOURCE_FILE_SCOPE));

    public NavDataInjectionDetector() {

    }

    @Override
    public boolean appliesTo(ResourceFolderType folderType) {
        return (folderType == ResourceFolderType.NAVIGATION || folderType == ResourceFolderType.LAYOUT);
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(SdkConstants.TAG_ACTIVITY, Constants.TAG_DEEPLINK, SdkConstants.TAG_NAVIGATION,Constants.TAG_ARGUMENT, Constants.TAG_WIDGET, Constants.TAG_FRAGMENT, Constants.TAG_FUN,SdkConstants.TAG_ACTION);
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        return Collections.singleton(SdkConstants.ATTR_NAV_GRAPH);
    }

    @Override
    public void afterCheckRootProject(Context context) {
        if(expOfIntrst != null && exported != null) {
            for(String screenId : exported) {
                if(malicious == null) malicious = new ArrayList<>();
                if(scope != null && scope.get(screenId) != null)
                    malicious.addAll(scope.get(screenId));
                if(hasNestedSynonym(screenId))
                    malicious.addAll(scope.get(syn_map.get(screenId)));
                if(hasActivitySynonym(screenId))
                    malicious
                            .addAll(scope
                                    .get(activity_layout_map
                                            .get(graph_layout_map
                                                    .get(graph_startDest_map
                                                            .get(screenId)))));
            }
            if(influences != null) {
                HashMap<String,List<String>> env = new HashMap<>();
                for(Pair<String,String> pair : influences) {
                    String key = pair.getFirst();
                    if(env.get(key) == null) {
                        List<String> v = new ArrayList<>();
                        v.add(pair.getSecond());
                        env.put(key,v);
                    }
                    else {
                        List<String> v = env.get(key);
                        v.add(pair.getSecond());
                        env.put(key,v);
                    }
                }

                for(Pair<String,String> pair : influences) {
                    String first = pair.getFirst();
                    String second = pair.getSecond();
                    if(influences_tran_closure == null) influences_tran_closure = new ArrayList<>();
                    influences_tran_closure.add(Pair.of(first,second));
                    List<String> reachableFromSecond = getReachableVars(second,env);
                    for (String s : reachableFromSecond) {
                        if(!contains(influences_tran_closure,Pair.of(first,s)))
                            influences_tran_closure.add(Pair.of(first,s));
                    }

                }
            }
            for (FunDao e : expOfIntrst) {
                String funName = e.getName();
                List<String> funArgs = e.getFunArgs();
                if(funArgs != null && malicious != null) {
                    for(String arg : funArgs) {
                        if(malicious.contains(arg))
                            context.report(DATA_INJECTION_SCREEN,e.getLocation(),arg + " used in " +
                                    funName + " might be malicious." +
                                    "Malicious inputs should not influence resources or entities accessed by your app");
                        else {
                            for (String mal : malicious) {
                                if(contains(influences_tran_closure,Pair.of(mal,arg))) {
                                    context.report(DATA_INJECTION_SCREEN,e.getLocation(),arg + " used in " +
                                            funName + " might be malicious." +
                                            "Malicious inputs should not influence resources or entities accessed by your app"); }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void visitAttribute(XmlContext context, Attr attr) {
        String fileNm = context.file.getName();
        String graph = attr.getValue();
        if(graph.contains("/")) {
            String key = graph.split("/")[1];
            if(graph_layout_map == null) graph_layout_map = new HashMap<>();
            graph_layout_map.put(key, fileNm.split(".xml")[0]);
        }
    }

    @Override
    public void visitElement(XmlContext context, Element element) {
        String name = element.getTagName();
        String elemId = "";
        String value = "";

        if(name.equals(SdkConstants.TAG_ACTIVITY)) {
            String id = element.getAttribute(Constants.ATTR_ID);
            String layout = element.getAttribute(Constants.ATTR_NAV_PARENT_LAYOUT);
            if(id.contains("/") && layout.contains("/")) {
                if(activity_layout_map == null) activity_layout_map = new HashMap<>();
                activity_layout_map.put(layout.split("/")[1], id.split("/")[1]);
            }
        }

        else if(name.equals(SdkConstants.TAG_NAVIGATION)) {
            String navId = element.getAttribute(Constants.ATTR_ID);
            String startDest = element.getAttribute(Constants.NAV_ATTR_START_DEST);
            String accessPol = element.getAttribute(Constants.NAV_ATTR_ACCESSPOLICY);
            String fileNm = context.file.getName();

            if(syn_map == null) syn_map = new HashMap<>();
            if(startDest.contains("/") && navId.contains("/")){
                syn_map.put(startDest.split("/")[1],navId.split("/")[1]);
                if(graph_startDest_map == null) graph_startDest_map = new HashMap<>();
                else graph_startDest_map.put(startDest.split("/")[1],fileNm.split(".xml")[0]);
            }
            else
                throw new IndexOutOfBoundsException("startDestination or navigation ID is missing.");

            if(accessPol.equals(Constants.ALL)) {
                if(startDest.contains("/")) {
                    if(exported == null) exported = new ArrayList<>();
                    exported.add(startDest.split("/")[1]);
                }
            }
        }
        else if(name.equals(Constants.TAG_DEEPLINK)) {
            Element parent = (Element) element.getParentNode();
            String parentId = parent.getAttribute(Constants.ATTR_ID);
            if(exported == null) exported = new ArrayList<>();
            if(parentId.contains("/"))
                exported.add(parentId.split("/")[1]);
        }
        else if(name.equals(Constants.TAG_FUN)) {
            String funName = element.getAttribute(Constants.NAV_ATTR_NAME);
            NodeList funParmNodeList = element.getElementsByTagName(Constants.TAG_PARM);
            List<String> parmArgs = UsefulMethods.getTagValues(funParmNodeList,Constants.NAV_ATTR_PARM_ARG,null);
            FunDao funDao = new FunDao(context.getLocation(element),funName,parmArgs);
            if(expOfIntrst == null) expOfIntrst = new ArrayList<>();
            expOfIntrst.add(funDao);
        }
        else if(name.equals(Constants.TAG_FRAGMENT)) {
            String screenId = element.getAttribute(Constants.ATTR_ID).split("/")[1];
            NodeList parentArguments = element.getElementsByTagName(Constants.TAG_ARGUMENT);
            List<String> parentArgNames = UsefulMethods.getTagValues(parentArguments,Constants.NAV_ATTR_NAME,
                    Collections.singletonList(SdkConstants.TAG_ACTION));
            if(parentArgNames != null) {
                updateScope(screenId,parentArgNames);
            }
        }
        else if(name.equals(SdkConstants.TAG_ACTION)) {
            NodeList actionArgumentTags = element.getElementsByTagName(Constants.TAG_ARGUMENT);
            List<String> actionArgumentNames = UsefulMethods.getTagValues(actionArgumentTags,Constants.NAV_ATTR_NAME,null);
            if(actionArgumentNames != null && element.getAttribute(Constants.NAV_ACTION_DEST).contains("/")) {
                String key = element.getAttribute(Constants.NAV_ACTION_DEST).split("/")[1];
                updateScope(key,actionArgumentNames);
            }
        }
        else if(name.equals(Constants.TAG_ARGUMENT)) {
            elemId = element.getAttribute(Constants.NAV_ATTR_NAME);
            value = element.getAttribute(Constants.NAV_ATTR_VAL_ARG);
        }
        else if(name.equals(Constants.TAG_WIDGET)
                && element.getAttribute(Constants.NAV_ATTR_WID).startsWith(SdkConstants.ID_PREFIX)) {
            elemId = element.getAttribute(Constants.NAV_ATTR_WID).split("/")[1];
            value = element.getAttribute(Constants.NAV_ATTR_WIDGET_VALUE);
        }
        if(value.startsWith(Constants.NAV_PREFIX_VAR) || value.startsWith(SdkConstants.ID_PREFIX)) {
            String var = value.split("/")[1];
            if(influences == null) influences = new ArrayList<>();
            influences.add(Pair.of(var,elemId));
        }
        else if(value.startsWith(Constants.NAV_PREFIX_FUN)) {
            NodeList parmNodes = element.getElementsByTagName(Constants.TAG_PARM);
            List<String> parmVals = UsefulMethods.getTagValues(parmNodes,Constants.NAV_ATTR_PARM_ARG,null);
            if(parmVals != null) {
                for(String parmVal : parmVals) {
                    if(influences == null) influences = new ArrayList<>();
                    influences.add(Pair.of(parmVal,elemId));
                }
            }
        }
    }

    private void updateScope(String key, List<String> value) {
        if(scope == null) scope = new HashMap<>();
        if(scope.get(key) == null) scope.put(key,value);
        else {
            List<String> l = scope.get(key);
            l.addAll(value);
            scope.put(key,l);
        }
    }

    private List<String> getReachableVars(String v, HashMap<String,List<String>> env) {
        List<String> result = new ArrayList<>();
        try {
            if (env.get(v) != null) {
                List<String> tmp = env.get(v);
                for (String next : tmp) {
                    if (!result.contains(next)) {
                        result.add(next);
                        List<String> res = getReachableVars(next, env);
                        if(!res.isEmpty()) result.addAll(res);
                    }
                }
                return result;
            }
            else
                return result;
        }
        catch(NullPointerException e) {
            System.out.println(result);
            throw e;
        }
    }

    private Boolean contains(List<Pair<String,String>> pairs, Pair<String,String> pair) {
        if(pairs != null && pair != null) {
            String first = pair.getFirst();
            String second = pair.getSecond();
            for (Pair<String,String> p : pairs) {
                if(p.getFirst().equals(first) && p.getSecond().equals(second))
                    return true;
            }
        }
        return false;
    }

    private Boolean hasNestedSynonym(String srcId) {
        return(syn_map != null && syn_map.get(srcId) != null
                && scope.get(syn_map.get(srcId)) != null);
    }

    private Boolean hasActivitySynonym(String srcId) {
        return(graph_startDest_map != null &&
                graph_layout_map != null &&
                activity_layout_map != null &&
                graph_startDest_map.get(srcId) != null &&
                graph_layout_map.get(graph_startDest_map.get(srcId)) != null &&
                activity_layout_map.get(graph_layout_map.get(graph_startDest_map.get(srcId))) != null);
    }
}

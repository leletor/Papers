import java.util.*;

/**
 * Created by junle
 */
public class VipTree {
    D2Dgraph G;
    Map<Integer,Node[]> nodesMap;//一个level对应一行的节点
    Map<Integer,Partition> parMap;
    Node[] nodes1;//叶子节点
    int pos; //查询Partition S对应叶节点的位置(暂时没用)，映射到叶节点
    Partition S;
    Partition T;
    Door midDoori,midDoorj;
    Node root;
    Map<FromTo,Door> midDoorMap = new HashMap<>();
    final static int t = 2;// t be the minimum degree of the IP-Tree denoting the minimum number of children in each
                           //non-root node

    VipTree(D2Dgraph d2Dgraph, Map<Integer,Door> doorMap,Map<Integer,Partition> parMap,Set<Partition>[] adjPar,
            Partition S,Partition T){
        G = d2Dgraph;
        this.parMap = parMap;
        this.S = S;
        this.T = T;
        Set<Door[]> doors = new LinkedHashSet<>();
        nodes1 = createLeafNode(parMap,adjPar);//获得叶子节点
        nodesMap = new HashMap<>();
        nodesMap.put(1,nodes1);
        int i = 1;
        while (nodesMap.get(i).length>1) {
            createNextLevel(nodesMap.get(i), t);
            i+=1;
        }
        root = nodesMap.get(i)[0];
    }

     class Node{
        int level;
        int degree;
        boolean isVisit = false;//在create next level中找最多公共门时候用到
        Set<Door> accessDoors ;
        Set<Partition> pSet;
        Node parent = null;
        Map<FromTo,Element> distanceMap;
        boolean isLeafNode = false;
        Set<Door> doors;
        Set<Node> child = new HashSet<>();
        Double minDist;
        Node(int l){
            level = l;
            degree = 1;
        }
        //叶子节点
        Node(int l,Set<Door> doors,Set<Partition> parS,boolean isLeafNode){
            degree = 1;
            this.doors = doors;//叶节点包含的所有门
            this.isLeafNode = isLeafNode;
            this.pSet = parS;//叶节点包含的所有分区
            Set<Door> aDoor = new HashSet<>();
            for (Door door:doors){
                if (door.isAccessDoor)
                    aDoor.add(door);
            }
            this.accessDoors = aDoor;//叶节点包含的所有access door
            distanceMap = new HashMap<>();//叶节点的距离矩阵，用map表示
            for (Door Ad:accessDoors)
                for (Door d:doors){
                    FromTo Ds = new FromTo(Ad,d);
                    if (Ad.equals(d)){
                        distanceMap.put(Ds,new Element(null,0));
                        continue;
                    }
                    Element E = G.getNextHopDoorAndDistance(Ad,d);
                    distanceMap.put(Ds,E);
                }
        }
    }

    public Node[] createLeafNode(Map<Integer,Partition> parMap,Set<Partition>[] adjPar){
        List<Node> nodeList = new ArrayList<>();
        for (int i=1;i < parMap.size();i++){
            Set<Partition> mergePartitions = new LinkedHashSet<>();
            Partition h = parMap.get(i);
            if (h.isHallWays){
                mergePartitions.add(h);    //感觉合并有更多公共门的走廊和分区复杂度很高。。。
                Set<Door> doors = new HashSet<>(h.doors);
                if (S.equals(h))
                    pos = nodeList.size();
                for (Partition P:adjPar[i]){
                    if (!P.isVisit&&!P.isHallWays&&P.pid!=0){
                        mergePartitions.add(P);
                        if (S.equals(P))
                            pos = nodeList.size();
                        P.isVisit = true;
                        doors.addAll(P.doors);
                    }
                }

                ////获得连接两个节点的access door,前面已经标记的access door是连接外面的
                for (Door door:doors){
                    Iterator<Partition> partitionIterator= door.pars.iterator();
                    Partition p1 = partitionIterator.next();
                    Partition p2 = partitionIterator.next();
                    if (!mergePartitions.contains(p1)||!mergePartitions.contains(p2)){
                        door.isAccessDoor = true;
                    }
                }
                nodeList.add(new Node(1,doors,mergePartitions,true));
            }
        }

        Node[] nodes1 = new Node[nodeList.size()];
        nodeList.toArray(nodes1);
        return nodes1;

    }

    public void createNextLevel(Node[] nodes, int t){
        Comparator<Node> nodeComparable = (o1, o2) -> o1.degree-o2.degree;
        PriorityQueue<Node> H = new PriorityQueue<>(nodeComparable);
        for (Node node : nodes) {
            H.add(node);
            node.degree = 1;
        }
        while (true){
            assert H.peek() != null;
            if (!(H.peek().degree < t)) break;
            Node N_i = H.remove();
            Node N_j = findHighestNumberCommonAccessDoor(N_i,nodes);
            H.remove(N_j);
            Node N_k = merge(N_i,N_j);
            H.add(N_k);
        }
        Node[] NL = new Node[H.size()];
        int j = 0;
        for (Node N:H){
            NL[j] = N;
            j+=1;
        }
        nodesMap.put(NL[0].level+1,NL);
    }

    private Node findHighestNumberCommonAccessDoor(Node N,Node[] nodes){
        Set<Door> D1 = N.accessDoors;
        N.isVisit = true;
        Node n = null;
        int maxInsertNum = 0;
        for (Node node:nodes){
            if (!node.isVisit){
                Set<Door> D = new HashSet<>(node.accessDoors);
                D.retainAll(D1);
                if (maxInsertNum < D.size()){
                    maxInsertNum = D.size();
                    n = node;
                }
            }
        }
        n.isVisit = true;
        return n;
    }

    public Node merge(Node N1,Node N2){
        Node node = new Node(N1.level+1);
        Set<Door> accessDoors = new HashSet<>();
        Set<Door> insertDoors = new HashSet<>();
        for (Door door:N1.accessDoors){
            for (Door door1:N2.accessDoors){
                accessDoors.add(door);
                accessDoors.add(door1);
                if (door1.equals(door))
                    insertDoors.add(door1);
            }
        }

        Map<FromTo,Element> distanceMap = new HashMap<>();
        for (Door door1:accessDoors)
            for (Door door2:accessDoors){
                FromTo Ds = new FromTo(door1,door2);
                if (door1.equals(door2)){
                    distanceMap.put(Ds,new Element(null,0));
                    continue;
                }
                Element E = G.getNextHopDoorAndDistance(door1,door2);
                distanceMap.put(Ds,E);
            }

        for (Door d:insertDoors){
            accessDoors.remove(d);
        }
        node.accessDoors = accessDoors;//l+1 level的access door
        node.distanceMap = distanceMap;
        node.degree = N1.degree+ N2.degree;
        node.pSet = new HashSet<>();
        node.pSet.addAll(N1.pSet);
        node.pSet.addAll(N2.pSet);
        N1.parent = node;
        N2.parent = node;
        node.child.add(N1);
        node.child.add(N2);
        return node;
    }

    public boolean isSuperior(Door d,Partition p,Node Leaf){
        Set<Door> globalDoor = new HashSet<>();
        for (Door D:Leaf.accessDoors){
            if (d.equals(D)&&p.doors.contains(d))
                return true;// local access door
            if (!D.isGlobal&&!p.doors.contains(D)&&Leaf.doors.contains(D))
                D.isGlobal = true;//global access door
        }
        for (Door door:Leaf.accessDoors){
            if (door.isGlobal){
                Door nextHopDoor = Leaf.distanceMap.get(new FromTo(d,door)).nextHopDoor;
                if (nextHopDoor==null||!p.doors.contains(nextHopDoor))
                    return true;
            }
        }
        return false;
    }

    //Shortest distance between s and an access door d that is in AD(Leaf(s)).

    public double getDis(Door s,Door d,Partition S){
        double min = Double.MAX_VALUE;
        Node Leaf = getLeafByPar(S);
        Door mid = null;
        for(Door di:S.doors){
            if (isSuperior(di,S,Leaf)){
                double dis1 = G.shortestPath(s,di);
                double dis2 = Leaf.distanceMap.get(new FromTo(di,d)).distance;
                if (min > dis1+dis2){
                    min = dis1+dis2;
                }
            }
        }
        return min;
    }

    public Node getLeafNode(Door s){
        for (Node node:nodes1){
            if (node.doors.contains(s))
                return node;
        }
        return null;
    }

//Shortest distance between s and all access doors of an ancestor of Leaf(s).
    public Map<FromTo,Double> getDistances(Door s,Node N,Partition S){
        //N is an ancestor node of Leaf(s)
        Node ns = getLeafNode(s);//Leaf(s)
        Node nParent =  ns.parent;
        Node nChild = ns;
        Map<FromTo,Double> m = new HashMap<>();
        Set<Door> markDoor = new HashSet<>();
        Door mid = null;
        while(!nChild.equals(N)){
            for (Door d: nParent.accessDoors){
                if (!d.isMarked){
                    double min = Double.MAX_VALUE;
                    for (Door d1:nChild.accessDoors){
                        double dis1,dis2;
                        if (m.containsKey(new FromTo(s,d1)))
                            dis1 = m.get(new FromTo(s,d1));
                        else
                            dis1 = getDis(s,d1,S);
                        if (m.containsKey(new FromTo(d1,d)))
                            dis2 = m.get(new FromTo(d1,d));
                        else
                            dis2 = nParent.distanceMap.get(new FromTo(d1,d)).distance;
                        double dis = dis1+dis2;
                        if (dis < min){
                            min = dis;
                            mid = d1;
                        }
                    }
                    m.put(new FromTo(s,d),min);
                    if (!mid.equals(s)&&!mid.equals(d))
                        midDoorMap.put(new FromTo(s,d),mid);
                    d.isMarked = true;
                    markDoor.add(d);
                }
            }
            for (Door door:markDoor)
                door.isMarked = false;
            nChild = nParent;
            nParent = nParent.parent;
        }
        return m;
    }

    private Node getLeafByPar(Partition partition){
        for (Node N:nodes1){
            if (N.pSet.contains(partition))
                return N;
        }
        return null;
    }


//Shortest distance between two arbitrary points s and t
//dist(s, t) when s and t are in different leaf nodes
    public double findArbitraryDis(Door s,Door t,Partition S,Partition T){
        Node Ns = getLeafByPar(S);
        Node Nt = getLeafByPar(T);
        if (Ns.equals(Nt))
            return getDis(s,t,S);
        Node Lca = LCA(Ns,Nt);
        if (Ns.parent.equals(Lca)&&Nt.parent.equals(Lca)){
            for (Door door:Ns.accessDoors){
                if (door.pars.contains(S)&&door.pars.contains(T)){
                    double dis = Ns.distanceMap.get(new FromTo(s,door)).distance;
                    double dis1 = Nt.distanceMap.get(new FromTo(door,t)).distance;
                    midDoori = door;
                    midDoorj = door;
                    return dis + dis1;
                }
            }
        }
        while (!Ns.parent.equals(Lca))
            Ns = Ns.parent;
        while (!Nt.parent.equals(Lca))
            Nt = Nt.parent;
        Map<FromTo,Double> m1 = getDistances(s,Ns,S);
        Map<FromTo,Double> m2 = getDistances(t,Nt,T);
        double min = Double.MAX_VALUE;
        // 当NS=Leaf(s),Nt = Leaf(t)
        if (m1.isEmpty()&&m2.isEmpty()){
            for (Door di:Ns.accessDoors)
                for (Door dj:Nt.accessDoors){
                    double dis1 = getDis(s,di,S);
                    double dis2 = Lca.distanceMap.get(new FromTo(di,dj)).distance;
                    double dis3 = getDis(t,dj,T);
                    double dis = dis1+dis2+dis3;
                    if (dis<min){
                        min = dis;
                        midDoori = di;
                        midDoorj = dj;
                    }
                }

        }
        else {
            for (Door di : Ns.accessDoors)
                for (Door dj : Nt.accessDoors) {
                    double dis1 = m1.get(new FromTo(s, di));
                    double dis2 = Lca.distanceMap.get(new FromTo(di, dj)).distance;
                    double dis3 = m2.get(new FromTo(t, dj));
                    double dis = dis1 + dis2 + dis3;
                    if (dis<min){
                        min = dis;
                        midDoori = di;
                        midDoorj = dj;
                    }
                }
        }

        return min;
    }

    public void printPath(Door s,Door t){
        Set<Door> doorSet = new LinkedHashSet<>();
        Door d1 = midDoorMap.get(new FromTo(s,midDoori));
        Door d2 = midDoorMap.get(new FromTo(midDoorj,t));
        if (d1==null){
            doorSet = Decompose(s,midDoori,doorSet);
        }
        else {
            doorSet = Decompose(s, d1, doorSet);
            doorSet = Decompose(d1, midDoori, doorSet);
        }
        if (!midDoori.equals(midDoorj)) {
            doorSet = Decompose(midDoori, midDoorj, doorSet);
        }
        if (d2==null){
            doorSet = Decompose(midDoorj,t,doorSet);
        }
        else {
            doorSet = Decompose(midDoorj, d2, doorSet);
            doorSet = Decompose(d2, t, doorSet);
        }

        System.out.print("the path :");
        for (Door door:doorSet)
            System.out.print("d"+door.label+" ");
        System.out.println();
    }

    //LCA(s, t) be the lowest common ancestor node
    //of Leaf(s) and Leaf(t).
    private Node LCA(Node s,Node t){
        while (!s.equals(t)){
            s = s.parent;
            t = t.parent;
        }
        return s;
    }
    //judge whether N is an ancestor of n
    private boolean isAncestor(Node n,Node N){
        while (n.parent!=null){
            if (n.parent.equals(N))
                return true;
        }
        return false;
    }

    //分解di和dj的路径
    public Set<Door> Decompose(Door di,Door dj,Set<Door> route){
        //判断是不是在同一叶节点，如果是直接算
        if (isInTheSameLeaf(S,T)){
            Stack<Door> doorStack = G.SPath(di,dj);
            while (!doorStack.isEmpty()){
                route.add(doorStack.pop());
            }
            return route;
        }
        //不在同一叶节点，或者在同一叶节点，但是该叶节点的access door
        if (di.equals(dj))
            route.add(di);
        else {
            if (!di.isAccessDoor && !dj.isAccessDoor) {
                route.add(di);
                route.add(dj);
            } else {
                Node N = null;
                if (di.isAccessDoor && dj.isAccessDoor) {
                    Node ni = getLeafNode(di);
                    Node nj = getLeafNode(dj);
                    N = LCA(ni, nj);
                } else {
                    for (Node node : nodes1) {
                        if (node.doors.contains(di) && node.doors.contains(dj))
                            N = node;
                    }
                }
                if (N==null){
                    Stack<Door> stack = G.SPath(di,dj);
                    while (!stack.isEmpty())
                        route.add(stack.pop());
                    return route;
                }

                if (!N.distanceMap.containsKey(new FromTo(di,dj))){
                    route.add(di);
                    route.add(dj);
                }
                else {
                    Door dk = N.distanceMap.get(new FromTo(di, dj)).nextHopDoor;
                    if (dk == null) {
                        route.add(di);
                        route.add(dj);
                    } else {
                        route = Decompose(di, dk, route);
                        route = Decompose(dk, dj, route);
                    }
                }
            }
        }
        return route;
    }
    private boolean isInTheSameLeaf(Partition S,Partition T){
        Node i = getLeafByPar(S);
        Node j = getLeafByPar(T);
        if (i.equals(j))
            return true;
        return false;
    }
    //找到q的KNN，Q为q所在的分区
    public PriorityQueue<Door> KNNs(Door q,int k,Partition Q){
        double dk = Double.MAX_VALUE;
        Map<FromTo,Double> m = getDistances(q,root,Q);
        Node leafQ = getLeafNode(q);
        for (Door d:leafQ.accessDoors){
            FromTo f = new FromTo(q,d);
            if (!m.containsKey(f))
                m.put(f,leafQ.distanceMap.get(f).distance);
        }
        while (leafQ.parent!=root){
            Map<FromTo,Double> mm = getDistances(q,leafQ.parent,Q);
            Iterator<Map.Entry<FromTo,Double>> iterator = mm.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<FromTo,Double> entry = iterator.next();
                FromTo f = entry.getKey();
                if (!m.containsKey(f))
                    m.put(f,entry.getValue());
            }
            leafQ = leafQ.parent;
        }
        Comparator<Node> nodeComparator = new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                return (int) (o1.minDist-o2.minDist);
            }
        };
        Comparator<Door> doorComparator = new Comparator<Door>() {
            @Override
            public int compare(Door o1, Door o2) {
                return (int) (G.shortestPath(q,o2)- G.shortestPath(q,o1));//距离越大排得越前
            }
        };
        PriorityQueue<Node> H = new PriorityQueue<>(nodeComparator);
        PriorityQueue<Door> pq = new PriorityQueue<>(doorComparator);
        H.add(root);
        while (!H.isEmpty()){
            Node N = H.remove();
            N.minDist = mindist(q,N,Q,m);
            if (N.minDist>dk)
                return pq;
            if (!N.isLeafNode){
                if (N.child!=null){
                    if (N.pSet.contains(Q)) {
                        for (Node node : N.child) {
                            node.minDist = mindist1(q, node, Q, m);
                            H.add(node);
                        }
                    }
                    else{
                        for (Node node : N.child) {
                            node.minDist = mindist2(q, node, Q, m);
                            H.add(node);
                        }
                    }
                }
            }
            else {
                for (Door door:N.doors) {
                    if (!door.isVisit) {
                        if (pq.size() < k) {
                            if (!door.equals(q)) {
                                pq.add(door);
                                door.isVisit = true;
                            }
                        } else {
                            if (!door.equals(q)) {
                                if (G.shortestPath(door,q)<G.shortestPath(pq.peek(),q)) {
                                    pq.poll();
                                    pq.add(door);
                                    door.isVisit = true;
                                }
                            }
                        }
                    }
                }
                dk = G.shortestPath(pq.peek(),q);

            }

        }
        return pq;
    }

    private double mindist(Door q,Node N,Partition Q,Map<FromTo,Double>map){
        double min = Double.MAX_VALUE;
        for (Door d:N.accessDoors){
            double dis;
            if (map.containsKey(new FromTo(q,d)))
                 dis = map.get(new FromTo(q,d));
            else
                dis = findArbitraryDis(q,d,Q,d.pars.iterator().next());
            if (dis<min)
                min = dis;
        }
        return min;
    }

    private double mindist1(Door q,Node N,Partition Q,Map<FromTo,Double> map){
        double dis = Double.MAX_VALUE;
        if (N.pSet.contains(Q))
            return 0;
        else {
            for (Node N1:N.parent.child){
                if (N1.pSet.contains(Q)){
                    for (Door di:N.accessDoors) {
                        double min = Double.MAX_VALUE;
                        for (Door dj : N1.accessDoors) {
                            double dis1 = map.get(new FromTo(q,dj));
                            double dis2 = N.parent.distanceMap.get(new FromTo(dj, di)).distance;
                            if (dis1 + dis2 < min) {
                                min = dis1 + dis2;
                            }
                        }
                        if (dis>min)
                            dis = min;
                    }
                }
            }
        }
        return dis;

    }

    private double mindist2(Door q,Node N,Partition Q,Map<FromTo,Double> map){
        double dis = Double.MAX_VALUE;
        if (N.pSet.contains(Q))
            return 0;
        else {
            Node N1 = N.parent;
            for (Door di:N.accessDoors){
                double min1 = Double.MAX_VALUE;
                for (Door dj : N1.accessDoors) {
                    double dis1 = map.get(new FromTo(q,dj));
                    double dis2 = N.parent.distanceMap.get(new FromTo(dj, di)).distance;
                    if (dis1 + dis2 < min1) {
                        min1 = dis1 + dis2;
                    }
                }
                if (dis>min1)
                    dis = min1;
            }
        }
        return dis;
    }

    void printKNN(PriorityQueue<Door> pq){
        Stack<Door> stack = new Stack<>();
        for (Door door:pq){
            stack.add(door);
        }
        while (!stack.isEmpty())
            System.out.print(" d"+stack.pop().label);
    }


}


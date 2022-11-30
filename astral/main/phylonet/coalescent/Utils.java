package phylonet.coalescent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import phylonet.bits.BitVector;
import phylonet.lca.SchieberVishkinLCA;
import phylonet.tree.io.NewickReader;
import phylonet.tree.io.ParseException;
import phylonet.tree.model.MutableTree;
import phylonet.tree.model.TMutableNode;
import phylonet.tree.model.TNode;
import phylonet.tree.model.Tree;
import phylonet.tree.model.sti.STINode;
import phylonet.tree.model.sti.STITree;
import phylonet.tree.model.sti.STITreeCluster;
import phylonet.tree.util.Bipartitions;
import phylonet.util.BitSet;

public class Utils {

	public static ArrayList<TNode> getChildrenAsList(TNode node) {
		ArrayList<TNode> children = new ArrayList<TNode>();
		for (TNode child : node.getChildren()) {
			children.add(child);
		}
		return children;
	}

	public static Tree buildTreeFromClusters(Iterable<STITreeCluster> clusters, TaxonIdentifier identifier ) {
        if ((clusters == null) || (!clusters.iterator().hasNext())) {
          throw new RuntimeException("Empty list of clusters. The function returns a null tree.");
        }
    
        //TaxonIdentifier spm = GlobalMaps.taxonNameMap.getSpeciesIdMapper().getSTTaxonIdentifier();
        MutableTree tree = new STITree<Double>();
    
        //String[] taxa = ((STITreeCluster)clusters.get(0)).getTaxa();
        for (int i = 0; i < identifier.taxonCount(); i++) {
          tree.getRoot().createChild(identifier.getTaxonName(i));
        }
    
        for (STITreeCluster tc : clusters) {
          if ((tc.getClusterSize() <= 1) || (tc.getClusterSize() == identifier.taxonCount()))
          {
            continue;
          }
    
          Set<TNode> clusterLeaves = new HashSet<TNode>();
          TNode node;
          for (String l : tc.getClusterLeaves()) {
            node = tree.getNode(l);
            clusterLeaves.add(node);
          }
    
          SchieberVishkinLCA lcaFinder = new SchieberVishkinLCA(tree);
          TNode lca = lcaFinder.getLCA(clusterLeaves);
    
          LinkedList<TNode> movedChildren = new LinkedList<TNode>();
          int nodes = clusterLeaves.size();
          for (TNode child : lca.getChildren()) {
            BitSet childCluster = new BitSet(identifier.taxonCount());
            for (TNode cl : child.getLeaves()) {
              int i = identifier.taxonId(cl.getName());
              childCluster.set(i);
            }
            
    
            BitSet temp = (BitSet)childCluster.clone();
            temp.and(tc.getBitSet());
            if (temp.equals(childCluster)) {
              movedChildren.add(child);
              nodes -= temp.cardinality();
            }
    
          }
          
          if (movedChildren.size() == 0 || nodes != 0) {
              continue;
          }
    
          STINode newChild = ((STINode)lca).createChild();
    
          while (!movedChildren.isEmpty()) {
            newChild.adoptChild((TMutableNode)movedChildren.get(0));
            movedChildren.remove(0);
          }
        }
    
        ((STITree<Double>)tree).setRooted(false);
        return (Tree)tree;
      }

    public static final void computeEdgeSupports(STITree<Double> support_tree, Iterable<Tree> trees) {
    
        // generate leaf assignment
        Hashtable<String,Integer> leaf_assignment = new Hashtable<String,Integer>();
        for(TNode n : support_tree.getNodes()) {
            if(n.isLeaf()) {
                leaf_assignment.put(n.getName(), leaf_assignment.size());
            }
        }
    
        // generate all the bipartitions
        Hashtable<BitVector,TNode> support_partitions = new Hashtable<BitVector,TNode>();
        Bipartitions.computeBipartitions(support_tree, leaf_assignment, support_partitions);
    
        LinkedList<Hashtable<BitVector,TNode>> tree_partitions = new LinkedList<Hashtable<BitVector,TNode>>();
        for(Tree t : trees) {
            Hashtable<BitVector,TNode> th = new Hashtable<BitVector,TNode>();
            Bipartitions.computeBipartitions(t, leaf_assignment, th);
            tree_partitions.add(th);
        }
    
        // compute the ratios
        for(Map.Entry<BitVector,TNode> e : support_partitions.entrySet()) {
            BitVector bvcomp = new BitVector(e.getKey());
            bvcomp.not();
    
            int count = 0;
    
            for(Hashtable<BitVector,TNode> h : tree_partitions) {
                if(h.containsKey(e.getKey()) || h.containsKey(bvcomp)) {
                    count++;
                }
            }
            if (!e.getValue().isLeaf())
                ((STINode<Double>) e.getValue()).setData(((double) count) / tree_partitions.size() * 100);
        }
    
        return;
    }

    public static final Tree greedyConsensus(Iterable<Tree> trees, boolean randomize,
    		TaxonIdentifier taxonIdentifier) {
    	return greedyConsensus(trees,new double[]{0d}, randomize, 1, taxonIdentifier).iterator().next();
    }
    
	
    public static List<Integer> getRange(int n) {
		List<Integer> range = new ArrayList<Integer>(n);
		for (int j = 0; j < n; j++) {
			range.add(j);
		}
		return range;
	}
	
    public static List<Integer> getOnes(int n) {
		List<Integer> range = new ArrayList<Integer>(n);
		for (int j = 0; j < n; j++) {
			range.add(1);
		}
		return range;
	}
    
    public static final Collection<Tree> greedyConsensus(Iterable<Tree> trees, 
    		double[] thresholds, boolean randomzie, int repeat, 
    		TaxonIdentifier taxonIdentifier) {
    
    	List<Tree> outTrees = new ArrayList<Tree>();

        HashMap<STITreeCluster, Integer> count = new HashMap<STITreeCluster, Integer>();
        int treecount = 0;
        for (Tree tree : trees) {
        	treecount++;
            List<STITreeCluster> geneClusters = Utils.getGeneClusters(tree, taxonIdentifier);
            for (STITreeCluster cluster: geneClusters) {

                if (count.containsKey(cluster)) {
                    count.put(cluster, count.get(cluster) + 1);
                    continue;
                }
            	STITreeCluster comp = cluster.complementaryCluster();
                if (count.containsKey(comp)) {
                    count.put(comp, count.get(comp) + 1);
                    continue;
                }
                count.put(cluster, 1);
            }
        }
        
        for (int gi = 0; gi < repeat; gi++) {
        	TreeSet<Entry<STITreeCluster,Integer>> countSorted = new 
        			TreeSet<Entry<STITreeCluster,Integer>>(new ClusterComparator(randomzie, taxonIdentifier.taxonCount()));
        
	        countSorted.addAll(count.entrySet());
	        
	        int ti = thresholds.length - 1;
	        double threshold = thresholds[ti];
	        List<STITreeCluster> clusters = new ArrayList<STITreeCluster>();   
	        for (Entry<STITreeCluster, Integer> entry : countSorted) {
	        	if (threshold > (entry.getValue()+.0d)/treecount) {	
	        		outTrees.add(0,Utils.buildTreeFromClusters(clusters, taxonIdentifier));
	        		ti--;
	        		if (ti < 0) {
	        			break;
	        		}
	        		threshold = thresholds[ti];
	        	}
	    		clusters.add(entry.getKey());
	        }
	        while (ti >= 0) {
	        	outTrees.add(0, Utils.buildTreeFromClusters(clusters, taxonIdentifier));
	    		ti--;
	        }
        }
        
        return outTrees;
    }

    
    public static List<STITreeCluster> getGeneClusters(Tree tree, 
    		TaxonIdentifier taxonIdentifier ){
        List<STITreeCluster> biClusters = new LinkedList<STITreeCluster>();
        Stack<BitSet> stack = new Stack<BitSet>();
        String[] leaves = taxonIdentifier.getAllTaxonNames();
        for (TNode node : tree.postTraverse()) {
            BitSet bs = new BitSet(leaves.length);
            if (node.isLeaf()) {
                // Find the index of this leaf.
                int i = taxonIdentifier.taxonId(node.getName());                
                bs.set(i);              
                stack.add(bs);
            }
            else {
                int childCount = node.getChildCount();
                BitSet[] childbslist = new BitSet[childCount];
                int index = 0;
                for (TNode child : node.getChildren()) {
                    BitSet childCluster = stack.pop();
                    bs.or(childCluster);
                    childbslist[index++] = childCluster;
                }             
                stack.add(bs);
            }
                          
            if(bs.cardinality()<leaves.length && bs.cardinality()>1){
                STITreeCluster tb = new STITreeCluster();
                tb.setCluster((BitSet)bs.clone());
                //if(!biClusters.contains(tb)){
                biClusters.add(tb);
                //}
            }
            
        }
        
        return biClusters;              
    }
    
    public static void main(String[] args) throws IOException{
        if ("--fixsupport".equals(args[0])) {
            String line;
            int l = 0;          
            BufferedReader treeBufferReader = new BufferedReader(new FileReader(args[1]));;
            List<Tree> trees = new ArrayList<Tree>();
            try {
                while ((line = treeBufferReader.readLine()) != null) {
                    l++;
                    if (line.length() > 0) {
                        line = line.replaceAll("\\)[^,);]*", ")");
                        NewickReader nr = new NewickReader(new StringReader(line));

                        Tree tr = nr.readTree();
                        trees.add(tr);
                        String[] leaves = tr.getLeaves();
                        for (int i = 0; i < leaves.length; i++) {
                            GlobalMaps.taxonIdentifier.taxonId(leaves[i]);
                        }
                    }
                }
                treeBufferReader.close();
            } catch (ParseException e) {
                treeBufferReader.close();
                throw new RuntimeException("Failed to Parse Tree number: " + l ,e);
            }
            int k = trees.size();
            System.err.println(k+" trees read from " + args[1]);
            STITree<Double> best = (STITree<Double>) trees.remove(k-1);
            STITree<Double> consensus = (STITree<Double>) trees.remove(k-2);
            
            for (TNode node : best.postTraverse()) {
                node.setParentDistance(TNode.NO_DISTANCE);
            }
            for (TNode node : consensus.postTraverse()) {
                node.setParentDistance(TNode.NO_DISTANCE);
            }
            
            Utils.computeEdgeSupports(consensus, trees);
            Utils.computeEdgeSupports(best, trees);
            trees.add(consensus);
            trees.add(best);
            
            String outfile = args[1] + ".autofixed.tre";
            BufferedWriter outbuffer;
            if (outfile == null) {
                outbuffer = new BufferedWriter(new OutputStreamWriter(System.out));
            } else {
                outbuffer = new BufferedWriter(new FileWriter(outfile));
            }
            for (Tree tree : trees) {
                outbuffer.write(tree.toStringWD()+ " \n");
            }
            outbuffer.flush();
            System.err.println("File "+args[1]+" fixed and saved as " + outfile);
        } else {
            System.err.println("Command " + args[0]+ " not found.");
        }
    }
    
	public static String getLeftmostLeaf(TNode from){
		for (TNode node : from.postTraverse()) {
			if (node.isLeaf()) {
				return node.getName();
			}
		}
		throw new RuntimeException("not possible");	
	}
	
	//TODO: change to an iterable
	public static List<BitSet> getBitsets(HashMap<String,Integer> randomSample,
			Tree restrictedTree) {
		
		ArrayList<BitSet> ret = new ArrayList<BitSet>();

		Stack<BitSet> stack = new Stack<BitSet>();
		for (TNode rgtn : restrictedTree.postTraverse()) {

			if (rgtn.isRoot() && rgtn.getChildCount() == 2) {
				continue;
			}
			BitSet bs = null;
			int legitchildcount = 0;
			if (rgtn.isLeaf()) {
				// Find the index of this leaf.
				if (randomSample.containsKey(rgtn.getName())) {
					bs = new BitSet(randomSample.size());
					int i =  randomSample.get(rgtn.getName());               
					bs.set(i); 
				}
			}
			else {
				int childCount = rgtn.getChildCount();
				for (int i = 0; i < childCount; i++) {
					BitSet pop = stack.pop();
					if (pop != null) {
						if (bs == null) {
							bs = new BitSet(randomSample.size());
						}
						bs.or(pop);
						legitchildcount++;
					}
				}
			}
			stack.push(bs);
			if (bs == null || legitchildcount < 2)
				continue;
			int bsc = bs.cardinality();
			if (bsc < 2 || bsc >= randomSample.size() - 1) {
				continue;
			}       
			ret.add(bs);
		}
		return ret;
	}
	
	/*public static void randomlyResolve(MutableTree tree) {
		for (TNode node : tree.postTraverse()) {
			if (node.getChildCount() < 3) {
				continue;
			}
			TNode first = node.getChildren().iterator().next();
			List<TNode> children = first.getSiblings();
			children.add(first);
			while (children.size() > 2) {
				TNode c1 = children.remove(GlobalMaps.random.nextInt(children.size()));
				TNode c2 = children.remove(GlobalMaps.random.nextInt(children.size()));
				TMutableNode mnode = (TMutableNode) node;
				TMutableNode newChild = mnode.createChild();
				newChild.adoptChild((TMutableNode) c1);
				newChild.adoptChild((TMutableNode) c2);
				children.add(newChild);
			}
		}
	}*/

	
	
	public static class ClusterComparator implements Comparator<Entry<STITreeCluster,Integer>> {
		private BSComparator bsComparator;

		public ClusterComparator (boolean randomize, int size) {
			this.bsComparator = new BSComparator(randomize, size);
		}

		@Override
		public int compare(Entry<STITreeCluster, Integer> o1,
				Entry<STITreeCluster, Integer> o2) {
			return this.bsComparator.compare(
					o1.getKey().getBitSet(),o1.getValue(),
					o2.getKey().getBitSet(),o2.getValue());
					
		}
	}

	public static class BSComparator implements Comparator<Entry<BitSet,Integer>> {

		//private boolean random;
		List<Integer> inds;
		public BSComparator (boolean randomize, int size) {
			inds = new ArrayList<Integer>(); 
			for (int i = 0; i < size; i++) {
				inds.add(i);
			}
			if (randomize) {
				Collections.shuffle(inds, GlobalMaps.random);
			}
		}
		@Override
		public int compare(Entry<BitSet, Integer> o1,
				Entry<BitSet, Integer> o2) {
			return compare(o1.getKey(),o1.getValue(), o2.getKey(), o2.getValue());
		}
		private int compare(BitSet k1, Integer v1, BitSet k2, Integer v2) {
			int a = v2.compareTo(v1);
			if (a != 0) {
				return a;
			}
			if  (k1.equals(k2)) {
				return 0;
			}
			for (int ind : this.inds) {
				boolean j = k1.get(ind);
				boolean jj = k2.get(ind);
				if (j != jj) {
					return (j) ? 1 : -1;
				} 
			}
			throw new RuntimeException("hmm! this should never be reached");
		}
	}
}
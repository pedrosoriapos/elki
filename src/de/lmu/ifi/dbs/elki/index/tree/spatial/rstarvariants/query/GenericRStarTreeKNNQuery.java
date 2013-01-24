package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.AbstractDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.DirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.DistanceEntry;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.query.GenericDistanceSearchCandidate;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.ComparableMinHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Instance of a KNN query for a particular spatial index.
 * 
 * Reference:
 * <p>
 * G. R. Hjaltason, H. Samet<br />
 * Ranking in spatial databases<br />
 * In: 4th Symposium on Advances in Spatial Databases, SSD'95
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses AbstractRStarTree
 * @apiviz.uses SpatialPrimitiveDistanceFunction
 */
@Reference(authors = "G. R. Hjaltason, H. Samet", title = "Ranking in spatial databases", booktitle = "Advances in Spatial Databases - 4th Symposium, SSD'95", url = "http://dx.doi.org/10.1007/3-540-60159-7_6")
public class GenericRStarTreeKNNQuery<O extends SpatialComparable, D extends Distance<D>> extends AbstractDistanceKNNQuery<O, D> {
  /**
   * The index to use
   */
  protected final AbstractRStarTree<?, ?> tree;

  /**
   * Spatial primitive distance function
   */
  protected final SpatialPrimitiveDistanceFunction<? super O, D> distanceFunction;

  /**
   * Constructor.
   * 
   * @param tree Index to use
   * @param distanceQuery Distance query to use
   */
  public GenericRStarTreeKNNQuery(AbstractRStarTree<?, ?> tree, SpatialDistanceQuery<O, D> distanceQuery) {
    super(distanceQuery);
    this.tree = tree;
    this.distanceFunction = distanceQuery.getDistanceFunction();
  }

  /**
   * Performs a k-nearest neighbor query for the given NumberVector with the
   * given parameter k and the according distance function. The query result is
   * in ascending order to the distance to the query object.
   * 
   * @param object the query object
   * @param knnList the knn list containing the result
   */
  protected void doKNNQuery(O object, KNNHeap<D> knnList) {
    final ComparableMinHeap<GenericDistanceSearchCandidate<D>> pq = new ComparableMinHeap<>(Math.min(knnList.getK() << 1, 20));

    // push root
    pq.add(new GenericDistanceSearchCandidate<>(distanceFunction.getDistanceFactory().nullDistance(), tree.getRootID()));
    D maxDist = distanceFunction.getDistanceFactory().infiniteDistance();

    // search in tree
    while(!pq.isEmpty()) {
      GenericDistanceSearchCandidate<D> pqNode = pq.poll();

      if(pqNode.mindist.compareTo(maxDist) > 0) {
        return;
      }
      maxDist = expandNode(object, knnList, pq, maxDist, pqNode.nodeID);
    }
  }

  private D expandNode(O object, KNNHeap<D> knnList, final ComparableMinHeap<GenericDistanceSearchCandidate<D>> pq, D maxDist, final int nodeID) {
    AbstractRStarTreeNode<?, ?> node = tree.getNode(nodeID);
    // data node
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        SpatialEntry entry = node.getEntry(i);
        D distance = distanceFunction.minDist(entry, object);
        tree.distanceCalcs++;
        if(distance.compareTo(maxDist) <= 0) {
          knnList.add(distance, ((LeafEntry) entry).getDBID());
          maxDist = knnList.getKNNDistance();
        }
      }
    }
    // directory node
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        SpatialEntry entry = node.getEntry(i);
        D distance = distanceFunction.minDist(entry, object);
        tree.distanceCalcs++;
        // Greedy expand, bypassing the queue
        if(distance.isNullDistance()) {
          expandNode(object, knnList, pq, maxDist, ((DirectoryEntry) entry).getPageID());
        }
        else {
          if(distance.compareTo(maxDist) <= 0) {
            pq.add(new GenericDistanceSearchCandidate<>(distance, ((DirectoryEntry) entry).getPageID()));
          }
        }
      }
    }
    return maxDist;
  }

  /**
   * Performs a batch knn query.
   * 
   * @param node the node for which the query should be performed
   * @param knnLists a map containing the knn lists for each query objects
   */
  protected void batchNN(AbstractRStarTreeNode<?, ?> node, Map<DBID, KNNHeap<D>> knnLists) {
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        SpatialEntry p = node.getEntry(i);
        for(Entry<DBID, KNNHeap<D>> ent : knnLists.entrySet()) {
          final DBID q = ent.getKey();
          final KNNHeap<D> knns_q = ent.getValue();
          D knn_q_maxDist = knns_q.getKNNDistance();

          DBID pid = ((LeafEntry) p).getDBID();
          // FIXME: objects are NOT accessible by DBID in a plain rtree context!
          D dist_pq = distanceQuery.distance(pid, q);
          if(dist_pq.compareTo(knn_q_maxDist) <= 0) {
            knns_q.add(dist_pq, pid);
          }
        }
      }
    }
    else {
      ModifiableDBIDs ids = DBIDUtil.newArray(knnLists.size());
      for(DBID id : knnLists.keySet()) {
        ids.add(id);
      }
      List<DistanceEntry<D, SpatialEntry>> entries = getSortedEntries(node, ids);
      for(DistanceEntry<D, SpatialEntry> distEntry : entries) {
        D minDist = distEntry.getDistance();
        for(Entry<DBID, KNNHeap<D>> ent : knnLists.entrySet()) {
          final KNNHeap<D> knns_q = ent.getValue();
          D knn_q_maxDist = knns_q.getKNNDistance();

          if(minDist.compareTo(knn_q_maxDist) <= 0) {
            SpatialEntry entry = distEntry.getEntry();
            AbstractRStarTreeNode<?, ?> child = tree.getNode(((DirectoryEntry) entry).getPageID().intValue());
            batchNN(child, knnLists);
            break;
          }
        }
      }
    }
  }

  /**
   * Sorts the entries of the specified node according to their minimum distance
   * to the specified objects.
   * 
   * @param node the node
   * @param ids the id of the objects
   * @return a list of the sorted entries
   */
  protected List<DistanceEntry<D, SpatialEntry>> getSortedEntries(AbstractRStarTreeNode<?, ?> node, DBIDs ids) {
    List<DistanceEntry<D, SpatialEntry>> result = new ArrayList<>();

    for(int i = 0; i < node.getNumEntries(); i++) {
      SpatialEntry entry = node.getEntry(i);
      D minMinDist = distanceQuery.getDistanceFactory().infiniteDistance();
      for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        D minDist = distanceFunction.minDist(entry, relation.get(iter));
        minMinDist = DistanceUtil.min(minDist, minMinDist);
      }
      result.add(new DistanceEntry<>(entry, minMinDist, i));
    }

    Collections.sort(result);
    return result;
  }

  @Override
  public KNNList<D> getKNNForObject(O obj, int k) {
    final KNNHeap<D> knnList = DBIDUtil.newHeap(distanceFunction.getDistanceFactory(), k);
    doKNNQuery(obj, knnList);
    return knnList.toKNNList();
  }

  @Override
  public List<KNNList<D>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    // While this works, it seems to be slow at least for large sets!
    final Map<DBID, KNNHeap<D>> knnLists = new HashMap<>(ids.size());
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      knnLists.put(DBIDUtil.deref(iter), DBIDUtil.newHeap(distanceFunction.getDistanceFactory(), k));
    }

    batchNN(tree.getRoot(), knnLists);

    List<KNNList<D>> result = new ArrayList<>();
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      result.add(knnLists.get(DBIDUtil.deref(iter)).toKNNList());
    }
    return result;
  }
}
package experimentalcode.students.goldschwendt;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;

public class BayesianInformationCriterion<V extends NumberVector, M extends MeanModel> extends InformationCriterion<V, M> {

  @Override
  public double evaluate(Relation<V> relation, Clustering<M> clustering, DistanceFunction<? super V> distanceFunction) {
    
    // number of all data points
    int n = 0;
    for (Cluster<M> aCluster : clustering.getAllClusters()) {
      n += aCluster.size();
    }
    
    // number of clusters
    int m = clustering.getAllClusters().size();
    
    // bayes information criterion for this clustering
    double bic =
      logLikelihoodClustering(relation, clustering, distanceFunction) -
      (m * Math.log(n)) / 2;
    
    return bic;
  }
}

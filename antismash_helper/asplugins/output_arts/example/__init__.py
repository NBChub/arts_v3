# vim: set fileencoding=utf-8 :
#

"""Example antiSMASH 2.0 output plugin

"""
import logging
from os import path
from antismash import utils


name = "example"
short_description = "Example ouptut"
# Output plugins are sorted by priority, lower numbers get run first
priority = 9


def write(seq_records, options):
    """Write all results to a file

    Args:
        seq_records (iterable): An iterable containing Bio.SeqRecords
        options (argparse.Namespace): The options passed to the program
    """
    basename = seq_records[0].id
    output_name = path.join(options.outputfoldername, "%s.example" % basename)
    logging.debug("Writing seq_records to %r" % output_name)
    with open(output_name, 'w') as handle:
        for rec in seq_records:
            id_ = rec.id
            num_clusters = len(utils.get_cluster_features(rec))
            handle.write("%s: %d clusters found\n" % (id_, num_clusters))

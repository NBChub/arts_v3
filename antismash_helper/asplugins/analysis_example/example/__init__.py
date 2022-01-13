# vim: set fileencoding=utf-8 :
#
"""example antiSMASH 2.0 analysis module

An analysis module has two functions that it should always implement and a
couple of functions that it can optionally implement.

The required functions are:
    check_prereqs - This is called to check if all prerequisits for the module
                    are met
    specific_analysis  - This is called to run the actual analysis code

The optional functions are all related to HTML output:
    will_handle - This allows a module to be called to generate output for
                  a cluster type
    generate_details_div - Generates the HTML output for cluster details part
                           of the results page
    generate_sidepanel - Generates HTML output for the results page side panel

"""
import logging
from antismash import utils
# This is required for the html output helper
from pyquery import PyQuery as pq

# name of the plugin
name = "example"
# short description for --list-plugins output
short_description = name.capitalize()

# The tuple is the name of the binary and whether it is an optional requirement
_required_binaries = [
        ('test', False),
    ]


def check_prereqs():
    """Check if all required binaries are available

    Returns:
        A list of error strings if prerequisites are not present
    """
    failure_messages = []
    for binary_name, optional in _required_binaries:
        if utils.locate_executable(binary_name) is None and not optional:
            failure_messages.append("Failed to locate executable for %r" %
                                    binary_name)
    return failure_messages


def specific_analysis(seq_record, options):
    """Run cluster-specific analysis here

    This function checks for the presence of "example" type clusters that were
    detected in the cluster finding stage. This is the likely use case, but
    you are not restricted to this.

    Args:
        seq_record (Bio.SeqRecord): Sequence record to run analysis on
        options (argpasrse.Namespace): Options passed to the executable
    """
    clusters = utils.get_cluster_features(seq_record)
    for cluster in clusters:
        if 'product' not in cluster.qualifiers or \
           'example' not in cluster.qualifiers['product'][0]:
            # not an "example" type cluster, ignore.
            continue

        logging.debug("Here you would implement the 'example' cluster analysis")


def will_handle(product):
    """Look at a cluster product string and decide if it should be handled

    Args:
        product (string): Product string of an identified cluster
    Returns:
        True if this module can handle clusters of this type
        False otherwise
    """
    # Just return True here if you really want to handle a cluster
    # For the sake of the example, always handle a cluster
    return True


def generate_details_div(cluster, seq_record, options, js_domains, details=None):
    """Generate cluster details output

    Args:
        cluster (dict): A dictionary representation of the current gene cluster
        seq_record (Bio.SeqRecord): The whole sequence record
        options (argpasrse.Namespace): Options passed to the executable
        js_domains (unused)
        details (PyQuery): pyquery object holding the details div
    Returns:
        the updated details div pyquery object
    """

    cluster_rec = utils.get_cluster_by_nr(seq_record, cluster['idx'])
    if cluster_rec is None:
        return details

    # if this is the first module to touch the details div, create it
    if details is None:
        details = pq('<div>')
        details.addClass('details')

        header = pq('<h3>')
        header.text('Detailed annotation')
        details.append(header)

    example = pq('<div>')
    example.text("Example cluster specific details output")
    details.append(example)

    return details


def generate_sidepanel(cluster, seq_record, options, sidepanel=None):
    """Generate sidepanel div

    Args:
        cluster (dict): A dictionary representation of the current gene cluster
        seq_record (Bio.SeqRecord): The whole sequence record
        options (argpasrse.Namespace): Options passed to the executable
        sidepanel (PyQuery): pyquery object holding the sidepanel div
    Returns:
        the updated sidepanel div pyquery object
    """
    cluster_rec = utils.get_cluster_by_nr(seq_record, cluster['idx'])
    if cluster_rec is None:
        return sidepanel

    if sidepanel is None:
        sidepanel = pq('<div>')
        sidepanel.addClass('sidepanel')

    example = pq('<div>')
    example.text("Example cluster specific details output")
    sidepanel.append(example)

    return sidepanel

__all__ = [ check_prereqs, specific_analysis, generate_sidepanel, generate_details_div, will_handle ]

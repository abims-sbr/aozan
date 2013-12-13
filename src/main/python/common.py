# -*- coding: utf-8 -*-

'''
Created on 25 oct. 2011

@author: Laurent Jourdren
'''

import smtplib, os.path, time
import mimetypes

from java.io import File, InputStreamReader
from java.io import BufferedReader
from java.lang import Runtime
from java.util.logging import Level
from com.google.common.io import CharStreams
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.mime.audio import MIMEAudio
from email.mime.base import MIMEBase
from email.mime.image import MIMEImage
from email import encoders

from fr.ens.transcriptome.aozan import Common
from fr.ens.transcriptome.aozan import Globals
from fr.ens.transcriptome.aozan.util import FileUtils



def df(path):
    """Get the free space on a partition.

    Arguments:
        path: file on the partition
    """
    # s = os.statvfs('/')
    # return (s.f_bavail * s.f_frsize)
    if os.path.exists(path):
        return long(File(path).getFreeSpace())

    return 0L

def du(path):
    """Get the disk usage of a directory.

    Arguments:
        path: path of the directory
    """
    cmd = 'du -b --max-depth=0 ' + path
    child_stdin, child_stdout = os.popen2(cmd)
    lines = child_stdout.readlines()
    child_stdin.close()
    child_stdout.close()

    return long(lines[0].split('\t')[0])


def send_msg(subject, message, is_error, conf):
    """Send a message to the user about the data extraction.
    
    Arguments:
        subject: subject of message
        message: text mail
        is_error: true if it is a error message
        conf: configuration object
    """


    send_mail = conf['send.mail'].lower() == 'true'
    smtp_server = conf['smtp.server']

    # Specific receiver for error message
    if is_error:
        mail_to = conf['mail.error.to']

        # Mail error not define
        if mail_to == None or mail_to == '':
            mail_to = conf['mail.to']
    else:
        mail_to = conf['mail.to']

    mail_from = conf['mail.from']
    mail_cc = None
    mail_bcc = None
    COMMASPACE = ', '

    message = conf['mail.header'].replace('\\n', '\n') + message + conf['mail.footer'].replace('\\n', '\n')
    message = message.replace('\n', '\r\n')
    msg = ''

    if mail_to != None :
        if type(mail_to) == str or type(mail_to) == unicode:
            mail_to = [mail_to]
        msg = msg + ("To: %s\r\n" % COMMASPACE.join(mail_to))

    if mail_cc != None :
        if type(mail_cc) == str or type(mail_cc) == unicode:
            mail_cc = [mail_cc]
        msg = msg + ("Cc: %s\r\n" % COMMASPACE.join(mail_cc))

    if mail_bcc != None :
        if type(mail_bcc) == str or type(mail_bcc) == unicode:
            mail_bcc = [mail_bcc]
        msg = msg + ("Bcc: %s\r\n" % COMMASPACE.join(mail_bcc))


    msg = msg + "Subject: " + subject + "\r\n" + message

    if send_mail:
        server = smtplib.SMTP(smtp_server)
        dests = []
        dests.extend(mail_to)
        if  mail_cc != None :
            dests.extend(mail_cc)
        if mail_bcc != None :
            dests.extend(mail_bcc)
        server.sendmail(mail_from, dests, msg)
        server.quit()
    else:
        print '-------------'
        print msg
        print '-------------'


def send_msg_with_attachment(subject, message, attachment_file, conf):
    """Send a message to the user about the data extraction."""


    send_mail = conf['send.mail'].lower() == 'true'
    smtp_server = conf['smtp.server']
    mail_to = conf['mail.to']
    mail_from = conf['mail.from']
    mail_cc = None
    mail_bcc = None
    COMMASPACE = ', '

    message = conf['mail.header'].replace('\\n', '\n') + message + conf['mail.footer'].replace('\\n', '\n')



    msg = MIMEMultipart()

    if mail_to != None :
        if type(mail_to) == str or type(mail_to) == unicode:
            mail_to = [mail_to]
        msg['To'] = COMMASPACE.join(mail_to)

    if mail_cc != None :
        if type(mail_cc) == str or type(mail_cc) == unicode:
            mail_cc = [mail_cc]
        msg['Cc'] = COMMASPACE.join(mail_cc)

    if mail_bcc != None :
        if type(mail_bcc) == str or type(mail_bcc) == unicode:
            mail_bcc = [mail_bcc]
        msg['Bcc'] = COMMASPACE.join(mail_bcc)

    msg['Subject'] = subject

    # Not seen
    msg.preamble = message

    # The message
    part1 = MIMEText(message, 'plain')
    msg.attach(part1)


    ctype, encoding = mimetypes.guess_type(attachment_file)

    if ctype is None or encoding is not None:
        # No guess could be made, or the file is encoded (compressed), so
        # use a generic bag-of-bits type.
        ctype = 'application/octet-stream'

    maintype, subtype = ctype.split('/', 1)
    if maintype == 'text':
        fp = open(attachment_file, 'r')
        # Note: we should handle calculating the charset
        part2 = MIMEText(fp.read(), _subtype=subtype)
        fp.close()
    elif maintype == 'image':
        fp = open(attachment_file, 'rb')
        part2 = MIMEImage(fp.read(), _subtype=subtype)
        fp.close()
    elif maintype == 'audio':
        fp = open(attachment_file, 'rb')
        part2 = MIMEAudio(fp.read(), _subtype=subtype)
        fp.close()
    else:
        fp = open(attachment_file, 'rb')
        part2 = MIMEBase(maintype, subtype)
        part2.set_payload(fp.read())
        fp.close()
        # Encode the payload using Base64
        encoders.encode_base64(part2)

    # Set the filename parameter
    part2.add_header('Content-Disposition', 'attachment', filename=os.path.basename(attachment_file))
    msg.attach(part2)


    # Now send or store the message
    composed = msg.as_string()

    if send_mail:
        server = smtplib.SMTP(smtp_server)
        dests = []
        dests.extend(mail_to)
        if  mail_cc != None :
            dests.extend(mail_cc)
        if mail_bcc != None :
            dests.extend(mail_bcc)
        server.sendmail(mail_from, dests, composed)
        server.quit()
    else:
        print '-------------'
        print composed
        print '-------------'


def error(short_message, message, last_error_file_path, conf):
    """Error handling.

    Arguments:
        short_message: short description of the message
        message: message
        conf: configuration dictionary
    """

    new_error = short_message + message
    new_error.replace('\n', ' ')
    log('SEVERE', new_error, conf)

    if os.path.exists(last_error_file_path):
        f = open(last_error_file_path, 'r')
        last_error = f.readline()
        f.close()

        if not new_error == last_error:
            send_msg(short_message, message, True, conf)
    else:
        send_msg(short_message, message, True, conf)

    f = open(last_error_file_path, 'w')
    f.write(new_error)
    f.close()


def log(level, message, conf):
    """Log message.
    
    Arguments:
        level: log level
        message: message to log
        conf: configuration dictionary
    """

    logger = Common.getLogger()
    logger.log(Level.parse(level), message)


def duration_to_human_readable(time):
    """Convert a number of seconds in human readable string.
    
    Arguments:
        time: the number of seconds
    """

    hours = int(time / 3600)
    hours_rest = time % 3600
    minutes = int(hours_rest / 60)
    minutes_rest = time % 60
    seconds = int(minutes_rest)

    return "%02d:%02d:%02d" % (hours, minutes, seconds)

def time_to_human_readable(time_since_epoch):
    """Convert a number of seconds since epoch in a human readable string.

    Arguments:
        time: the number of seconds
    """

    return time.strftime("%a %b %d %H:%M:%S %Z %Y", time.localtime(time_since_epoch))

def load_processed_run_ids(done_file_path):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    result = set()

    if not os.path.exists(done_file_path):
        return result

    f = open(done_file_path, 'r')

    for l in f:
        run_id = l[:-1]
        if len(run_id) == 0:
            continue
        result.add(run_id)

    f.close()

    return result


def add_run_id_to_processed_run_ids(run_id, done_file_path, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run id: The run id
        done_file_path: path of the done file
        conf: configuration dictionary
    """

    log('INFO', 'Add ' + run_id + ' to ' + os.path.basename(done_file_path), conf)

    f = open(done_file_path, 'a')

    f.write(run_id + '\n')

    f.close()


def load_conf(conf, conf_file_path):
    """Load configuration file"""

    # in version Aozan 1.1.1 change key in configuration to replace design by sample sheet
    # converting table between old and new key
    converting_table_key = {}
    converting_table_key['casava.design.format'] = 'casava.samplesheet.format'
    converting_table_key['casava.design.prefix.filename'] = 'casava.samplesheet.prefix.filename'
    converting_table_key['casava.designs.path'] = 'casava.samplesheets.path'

    f = open(conf_file_path, 'r')

    for l in f:
        s = l[:-1].strip()
        if len(s) == 0 or l[0] == '#' :
            continue
        fields = s.split('=')
        if len(fields) == 2:
            conf[fields[0].strip()] = fields[1].strip()

            # Check if needed to converting key for design fields
            if fields[0].strip() in converting_table_key:
                conf[converting_table_key[fields[0].strip()]] = fields[1].strip()
    f.close()
    return conf

def create_html_index_file(conf, output_file_path, run_id, sections):
    """Create an index.html file that contains links to all the generated reports.

    Arguments:
        conf: configuration dictionary
        output_file_path: path of the index.html file to create
        run_id: The run id
        sections: The list of section to write
    """

    """ Since version RTA after 1.17, Illumina stop the generation of the Status and reports files"""

    path_report = conf['reports.data.path'] + '/' + run_id
    
    # Retrieve BufferedReader on index html template
    template_path = conf['index.html.template']
    if template_path != None and template_path != '' and os.path.exists(template_path):
        f_in = open(template_path, 'r')
        text = ''.join(f_in.readlines())
        lines = text.split('\n');
        f_in.close()
    
    else:
        # Use default template save in aozan jar file
        jar_is = Globals.getResourceAsStream(Globals.INDEX_HTML_TEMPLATE_FILENAME)
        lines = FileUtils.readFileByLines(jar_is)
    
    if 'sync' in sections and os.path.exists(path_report + '/report_' + run_id):
        sections.append('optional')
            
    write_lines = True
    result = ''

    for line in lines:
        if line.startswith('<!--START_SECTION'):
            section_name = line.split(' ')[1]
            if section_name in sections:
                write_lines = True
            else:
                write_lines = False
        elif line.startswith('<!--END_SECTION'):
            write_lines = True
        
        elif write_lines == True:
            if '${RUN_ID}' in line:
                result += line.replace('${RUN_ID}', run_id) + '\n'
            elif '${VERSION}' in line:
                result += line.replace('${VERSION}', Globals.APP_VERSION_STRING) + '\n'
            else:
                result += line + '\n' 

    f_out = open(output_file_path, 'w')
    f_out.write(result)
    f_out.close()


def set_default_conf(conf):

    # Global
    conf['aozan.enable'] = 'True'
    conf['send.mail'] = 'False'
    conf['aozan.log.level'] = str(Globals.LOG_LEVEL)
    conf['first.base.report.step'] = 'True'
    conf['hiseq.step'] = 'True'
    conf['sync.step'] = 'True'
    conf['demux.step'] = 'True'
    conf['qc.step'] = 'False'

    # Lock file
    conf['lock.file'] = '/var/lock/aozan.lock'

    # Synchronization
    conf['sync.exclude.cif'] = 'True'
    conf['sync.continuous.sync'] = 'False'
    conf['sync.continuous.sync.min.age.files'] = '15'

    # Casava
    conf['demux.use.hiseq.output'] = 'False'
    conf['casava.samplesheet.format'] = 'xls'
    conf['casava.samplesheet.prefix.filename'] = 'design'
    conf['casava.path'] = '/usr/local/casava'
    conf['casava.compression'] = 'bzip2'
    conf['casava.fastq.cluster.count'] = '0'
    conf['casava.compression.level'] = '9'
    conf['casava.mismatches'] = '0'
    conf['casava.threads'] = str(Runtime.getRuntime().availableProcessors())
    conf['casava.adapter.fasta.file.path'] = ''
    conf['casava.with.failed.reads'] = 'True'
    conf['casava.additionnal.arguments'] = ''

    # Data path
    conf['tmp.path'] = '/tmp'
    conf['index.sequences'] = ''
    conf['index.html.template'] = ''
    conf['reports.url'] = ''
    conf['qc.report.stylesheet'] = ''

    # Space needed
    conf['hiseq.warning.min.space'] = str(3 * 1024 * 1024 * 1024 * 1024)
    conf['hiseq.critical.min.space'] = str(1 * 1024 * 1024 * 1024 * 1024)
    conf['sync.space.factor'] = str(0.2)
    conf['demux.space.factor'] = str(0.7)
    # Value for step estimated space needed during first base report
    # estimation factor for fastq_space_per_lane_per_cycle (cmd du -b)
    conf['fastq.space.factor'] = str(224000000)
    # estimation factor for bcl_space_per_lane_per_cycle (cmd du -b)
    conf['bcl.space.factor'] = str(416000000)
    # estimation factor for hiseq_space_per_lane_per_cycle (cmd du -b)
    conf['hiseq.space.factor'] = str(3180000000)


    # Mail configuration
    conf['mail.header'] = 'THIS IS AN AUTOMATED MESSAGE.\\n\\n'
    conf['mail.footer'] = '\\n\\nThe Aozan team.\\n'

    # Collectors configuration
    # ReadCollector
    conf['qc.conf.read.xml.collector.used'] = 'false'
    # TileMetricsCollector
    conf['qc.conf.cluster.density.ratio'] = str(0.3472222)
    # TemporaryPartialFastqCollector
    conf['qc.conf.reads.pf.used'] = str(200000)
    # Use only the first X reads pf in fastq file
    conf['qc.conf.max.reads.parsed'] = str(30000000)
    # Configuration FastqscreenCollector, parameters for mapping
    # no detection contamination for control lane
    conf['qc.conf.skip.control.lane'] = 'true'
    # run paired : no paired mapping
    conf['qc.conf.ignore.paired.mode'] = 'true'

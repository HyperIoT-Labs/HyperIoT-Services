/*
 * Copyright 2019-2023 ACSoftware
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

(function () {

  /**
   * Sets the initial sorting derived from the hash.
   *
   * @param linkelementids
   *          list of element ids to search for links to add sort inidcator
   *          hash links
   */
  function initialSort(linkelementids) {
    window.linkelementids = linkelementids;
    var hash = window.location.hash;
    if (hash) {
      var m = hash.match(/up-./);
      if (m) {
        var header = window.document.getElementById(m[0].charAt(3));
        if (header) {
          sortColumn(header, true);
        }
        return;
      }
      var m = hash.match(/dn-./);
      if (m) {
        var header = window.document.getElementById(m[0].charAt(3));
        if (header) {
          sortColumn(header, false);
        }
        return
      }
    }
  }

  /**
   * Sorts the columns with the given header dependening on the current sort state.
   */
  function toggleSort(header) {
    var sortup = header.className.indexOf('down ') == 0;
    sortColumn(header, sortup);
  }

  /**
   * Sorts the columns with the given header in the given direction.
   */
  function sortColumn(header, sortup) {
    var table = header.parentNode.parentNode.parentNode;
    var body = table.tBodies[0];
    var colidx = getNodePosition(header);

    resetSortedStyle(table);

    var rows = body.rows;
    var sortedrows = [];
    for (var i = 0; i < rows.length; i++) {
      r = rows[i];
      sortedrows[parseInt(r.childNodes[colidx].id.slice(1))] = r;
    }

    var hash;

    if (sortup) {
      for (var i = sortedrows.length - 1; i >= 0; i--) {
        body.appendChild(sortedrows[i]);
      }
      header.className = 'up ' + header.className;
      hash = 'up-' + header.id;
    } else {
      for (var i = 0; i < sortedrows.length; i++) {
        body.appendChild(sortedrows[i]);
      }
      header.className = 'down ' + header.className;
      hash = 'dn-' + header.id;
    }

    setHash(hash);
  }

  /**
   * Adds the sort indicator as a hash to the document URL and all links.
   */
  function setHash(hash) {
    window.document.location.hash = hash;
    ids = window.linkelementids;
    for (var i = 0; i < ids.length; i++) {
        setHashOnAllLinks(document.getElementById(ids[i]), hash);
    }
  }

  /**
   * Extend all links within the given tag with the given hash.
   */
  function setHashOnAllLinks(tag, hash) {
    links = tag.getElementsByTagName("a");
    for (var i = 0; i < links.length; i++) {
        var a = links[i];
        var href = a.href;
        var hashpos = href.indexOf("#");
        if (hashpos != -1) {
            href = href.substring(0, hashpos);
        }
        a.href = href + "#" + hash;
    }
  }

  /**
   * Calculates the position of a element within its parent.
   */
  function getNodePosition(element) {
    var pos = -1;
    while (element) {
      element = element.previousSibling;
      pos++;
    }
    return pos;
  }

  /**
   * Remove the sorting indicator style from all headers.
   */
  function resetSortedStyle(table) {
    for (var c = table.tHead.firstChild.firstChild; c; c = c.nextSibling) {
      if (c.className) {
        if (c.className.indexOf('down ') == 0) {
          c.className = c.className.slice(5);
        }
        if (c.className.indexOf('up ') == 0) {
          c.className = c.className.slice(3);
        }
      }
    }
  }

  window['initialSort'] = initialSort;
  window['toggleSort'] = toggleSort;

})();

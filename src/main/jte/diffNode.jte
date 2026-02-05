@import one.dastec.jcompare.model.DiffNode
@param String leftPath
@param String rightPath
@param DiffNode node

<li class="status-${node.getStatus().name().toLowerCase()} flex flex-col">
    <div class="flex items-center space-x-2 ${node.isDirectory() ? "" : "cursor-pointer hover:bg-blue-50"}"
         data-left="${leftPath}"
         data-right="${rightPath}"
         data-rel="${node.getRelativePath()}"
         ondblclick="if(this.dataset.rel) openFileDiff(this.dataset.left, this.dataset.right, this.dataset.rel)">
        <span class="${node.isDirectory() ? "folder" : "file"}"></span>
        <span class="text-sm font-medium">${node.getName()}</span>
        <span class="text-xs uppercase px-2 py-0.5 rounded bg-gray-100">${node.getStatus()}</span>
    </div>
    @if(!node.getChildren().isEmpty())
        <ul class="ml-6 border-l border-gray-200 pl-4 mt-1 space-y-1">
            @for(DiffNode child : node.getChildren())
                @template.diffNode(node = child, leftPath = leftPath, rightPath = rightPath)
            @endfor
        </ul>
    @endif
</li>
